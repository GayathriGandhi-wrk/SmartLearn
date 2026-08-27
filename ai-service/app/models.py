"""
Model training and management.

Trains four models:
  - Random Forest
  - Gradient Boosting
  - XGBoost
  - Decision Tree

Predicts a student score (regression) and derives grade/risk. If no real dataset
is provided, a synthetic dataset is generated so the service works out of the box.
"""

import logging
import os
import pickle

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.tree import DecisionTreeRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_absolute_error
import joblib

from app.preprocessing import DataPreprocessor, FEATURE_COLUMNS, map_score_to_grade, map_score_to_risk

logger = logging.getLogger("models")

MODEL_NAMES = ["random_forest", "gradient_boosting", "xgboost", "decision_tree"]


class ModelManager:
    """Loads, trains and exposes ML models for prediction."""

    def __init__(self, model_dir: str = "models"):
        self.model_dir = model_dir
        os.makedirs(model_dir, exist_ok=True)
        self.models = {}
        self.metrics = {}
        self.preprocessor = DataPreprocessor()
        self._loaded = False

    # ------------------------------------------------------------------ #
    # Synthetic dataset (used when no real data is available)            #
    # ------------------------------------------------------------------ #
    @staticmethod
    def generate_synthetic_dataset(n: int = 2000, seed: int = 42) -> pd.DataFrame:
        rng = np.random.default_rng(seed)

        cgpa = rng.uniform(4.0, 9.8, n)
        attendance = rng.uniform(55, 100, n)
        study_hours = rng.uniform(1, 15, n)
        assignments = rng.uniform(40, 100, n)
        prev_score = rng.uniform(35, 100, n)
        attempted = rng.integers(10, 300, n).astype(float)
        accuracy = rng.uniform(30, 100, n)
        semester = rng.integers(1, 9, n).astype(float)
        streak = rng.integers(0, 30, n).astype(float)
        participation = rng.uniform(30, 100, n)

        base = (
            0.45 * (cgpa / 10) * 100
            + 0.15 * attendance
            + 0.10 * np.clip(study_hours * 8, 0, 100)
            + 0.08 * assignments
            + 0.07 * prev_score
            + 0.10 * accuracy
            + 0.05 * participation
        )
        noise = rng.normal(0, 6, n)
        score = np.clip(base + noise, 20, 100)

        correct = (attempted * accuracy / 100).astype(int)
        df = pd.DataFrame({
            "cgpa": cgpa,
            "attendance": attendance,
            "study_hours_per_week": study_hours,
            "assignments_completed": assignments,
            "previous_test_score": prev_score,
            "total_questions_attempted": attempted,
            "total_correct": correct,
            "total_incorrect": attempted - correct,
            "accuracy": accuracy,
            "semester": semester,
            "streak_days": streak,
            "participation_score": participation,
            "score": score,
        })
        return df

    # ------------------------------------------------------------------ #
    # Training                                                           #
    # ------------------------------------------------------------------ #
    def ensure_models(self, preprocessor: DataPreprocessor):
        """Train models if no saved model exists, otherwise load them."""
        if self._loaded:
            return
        if self._models_exist():
            self._load_models(preprocessor)
            return
        logger.info("No saved models found - training from synthetic dataset...")
        df = self.generate_synthetic_dataset()
        self.train(df, preprocessor)
        self.save()

    def train(self, df: pd.DataFrame, preprocessor: DataPreprocessor):
        data = df.copy()
        # Handle any subject_* extra columns by normalising to per-subject accuracy feature
        extra = [c for c in data.columns if c.startswith("subject_") and c not in FEATURE_COLUMNS]
        if extra:
            data["subject_accuracy"] = data[extra].mean(axis=1)
        y = data["score"].astype(float)

        preprocessor.fit(data, y.apply(map_score_to_grade))
        X = preprocessor.transform(data)
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42)

        builders = {
            "random_forest": lambda: RandomForestRegressor(
                n_estimators=200, max_depth=12, random_state=42),
            "gradient_boosting": lambda: GradientBoostingRegressor(
                n_estimators=180, learning_rate=0.08, max_depth=5, random_state=42),
            "xgboost": lambda: self._build_xgboost(),
            "decision_tree": lambda: DecisionTreeRegressor(max_depth=10, random_state=42),
        }

        for name, build in builders.items():
            try:
                model = build()
                model.fit(X_train, y_train)
                preds = model.predict(X_test)
                self.metrics[name] = {
                    "r2": round(float(r2_score(y_test, preds)), 4),
                    "mae": round(float(mean_absolute_error(y_test, preds)), 4),
                }
                self.models[name] = model
                logger.info("Trained %s -> r2=%.4f mae=%.4f",
                            name, self.metrics[name]["r2"], self.metrics[name]["mae"])
            except Exception as exc:  # pragma: no cover
                logger.warning("Failed to train %s: %s", name, exc)

        self.preprocessor = preprocessor
        self._loaded = True

    def _build_xgboost(self):
        try:
            from xgboost import XGBRegressor
            return XGBRegressor(n_estimators=180, learning_rate=0.08,
                                max_depth=5, random_state=42, verbosity=0)
        except ImportError:
            from sklearn.ensemble import ExtraTreesRegressor
            return ExtraTreesRegressor(n_estimators=180, random_state=42)

    # ------------------------------------------------------------------ #
    # Persistence                                                        #
    # ------------------------------------------------------------------ #
    def _models_exist(self) -> bool:
        return all(os.path.exists(os.path.join(self.model_dir, f"{name}.joblib"))
                   for name in MODEL_NAMES)

    def save(self):
        for name, model in self.models.items():
            joblib.dump(model, os.path.join(self.model_dir, f"{name}.joblib"))
        joblib.dump(self.preprocessor, os.path.join(self.model_dir, "preprocessor.joblib"))
        with open(os.path.join(self.model_dir, "metrics.json"), "w") as f:
            import json
            json.dump(self.metrics, f, indent=2)
        logger.info("Models saved to %s", self.model_dir)

    def _load_models(self, preprocessor: DataPreprocessor):
        try:
            self.preprocessor = joblib.load(os.path.join(self.model_dir, "preprocessor.joblib"))
        except Exception:
            self.preprocessor = preprocessor
        for name in MODEL_NAMES:
            try:
                self.models[name] = joblib.load(os.path.join(self.model_dir, f"{name}.joblib"))
            except Exception as exc:
                logger.warning("Failed to load %s: %s", name, exc)
        try:
            import json
            with open(os.path.join(self.model_dir, "metrics.json")) as f:
                self.metrics = json.load(f)
        except Exception:
            self.metrics = {}
        self._loaded = True

    # ------------------------------------------------------------------ #
    # Prediction                                                         #
    # ------------------------------------------------------------------ #
    def predict(self, features: dict, model_name: str = "ensemble") -> dict:
        df = pd.DataFrame([features])
        X = self.preprocessor.transform(df)
        preds = {}

        if model_name == "ensemble" or model_name not in self.models:
            for name, model in self.models.items():
                preds[name] = float(np.clip(model.predict(X)[0], 0, 100))
            score = float(np.mean(list(preds.values())))
        else:
            score = float(np.clip(self.models[model_name].predict(X)[0], 0, 100))
            preds[model_name] = score

        confidence = self._confidence(score, preds)
        return {
            "model_name": "ensemble" if model_name == "ensemble" else model_name,
            "predicted_score": round(score, 2),
            "predicted_grade": map_score_to_grade(score),
            "risk_level": map_score_to_risk(score),
            "confidence_score": round(confidence, 2),
            "per_model_predictions": {k: round(v, 2) for k, v in preds.items()},
            "feature_importance": self.feature_importance("random_forest"),
            "metrics": self.metrics,
        }

    def _confidence(self, score: float, preds: dict) -> float:
        if len(preds) > 1:
            values = list(preds.values())
            spread = np.std(values)
            return max(50, min(99, 95 - spread * 2))
        return 90.0

    def feature_importance(self, model_name: str = "random_forest") -> dict:
        model = self.models.get(model_name)
        if model is None:
            return {}
        if hasattr(model, "feature_importances_"):
            importances = model.feature_importances_
            return {FEATURE_COLUMNS[i]: float(importances[i])
                    for i in range(min(len(FEATURE_COLUMNS), len(importances)))}
        return {}
