"""
Explainable AI module (SHAP + LIME).

Generates model-agnostic explanations for a single prediction:
  - SHAP: tree explainer for Random Forest / GBM / XGBoost
  - LIME: local surrogate explainer
Both return per-feature contribution values and a human readable explanation.
"""

import logging
import warnings

import numpy as np
import pandas as pd

from app.preprocessing import FEATURE_COLUMNS

logger = logging.getLogger("explainability")
warnings.filterwarnings("ignore")


class Explainer:
    """Compute SHAP / LIME explanations for predictions."""

    def __init__(self, model_manager):
        self.model_manager = model_manager
        self._shap_explainer = None

    def shap(self, features: dict, model_name: str = "random_forest") -> dict:
        model = self.model_manager.models.get(model_name)
        if model is None:
            model = next(iter(self.model_manager.models.values()))
        X = self.model_manager.preprocessor.transform(pd.DataFrame([features]))
        try:
            import shap
            if self._shap_explainer is None:
                background = self._background_samples()
                self._shap_explainer = shap.TreeExplainer(model, background)
            values = self._shap_explainer.shap_values(X)[0]
            if isinstance(values, list):
                values = np.array(values)[0]
            contributions = {FEATURE_COLUMNS[i]: float(values[i])
                             for i in range(min(len(FEATURE_COLUMNS), len(values)))}
            explanation = _build_explanation(contributions)
            return {"method": "shap", "contributions": contributions,
                    "base_value": float(self._shap_explainer.expected_value
                                        if not isinstance(self._shap_explainer.expected_value, (list, np.ndarray))
                                        else np.mean(self._shap_explainer.expected_value)),
                    "explanation": explanation}
        except Exception as exc:
            logger.warning("SHAP failed: %s", exc)
            return {"method": "shap", "contributions": {}, "base_value": 0,
                    "explanation": "SHAP explanation unavailable for this request."}

    def lime(self, features: dict, model_name: str = "random_forest") -> dict:
        try:
            from lime.lime_tabular import LimeTabularExplainer
            model = self.model_manager.models.get(model_name)
            if model is None:
                model = next(iter(self.model_manager.models.values()))
            background = self._background_samples()
            explainer = LimeTabularExplainer(
                background,
                feature_names=FEATURE_COLUMNS,
                mode="regression",
                random_state=42)
            X = self.model_manager.preprocessor.transform(pd.DataFrame([features]))
            exp = explainer.explain_instance(X[0], model.predict, num_features=len(FEATURE_COLUMNS))
            contributions = dict(exp.as_map()[1])
            named = {}
            for idx, weight in contributions.items():
                if idx < len(FEATURE_COLUMNS):
                    named[FEATURE_COLUMNS[idx]] = float(weight)
            explanation = _build_explanation(named)
            return {"method": "lime", "contributions": named,
                    "explanation": explanation,
                    "intercept": float(exp.intercept[1])}
        except Exception as exc:
            logger.warning("LIME failed: %s", exc)
            return {"method": "lime", "contributions": {}, "intercept": 0,
                    "explanation": "LIME explanation unavailable for this request."}

    def _background_samples(self, n: int = 50) -> np.ndarray:
        df = self.model_manager.generate_synthetic_dataset(n=n, seed=7)
        return self.model_manager.preprocessor.transform(df)

    def importance(self) -> dict:
        return self.model_manager.feature_importance("random_forest")


def _build_explanation(contributions: dict) -> str:
    """Human readable explanation from feature contributions."""
    if not contributions:
        return "Prediction is primarily driven by the overall academic profile."
    ordered = sorted(contributions.items(), key=lambda kv: abs(kv[1]), reverse=True)
    top3 = ordered[:3]
    parts = []
    for name, value in top3:
        direction = "boosting" if value > 0 else "reducing"
        parts.append(f"{name} ({direction} by {abs(value):.2f})")
    return "Key factors: " + ", ".join(parts) + "."
