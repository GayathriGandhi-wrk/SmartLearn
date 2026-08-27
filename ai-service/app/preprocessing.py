"""
Data Preprocessing module.

Handles:
  - Missing value imputation
  - Standard scaling
  - Categorical encoding
  - Feature transformation / clipping
"""

import logging

import numpy as np
import pandas as pd
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler, LabelEncoder

logger = logging.getLogger("preprocessing")

NUMERIC_FEATURES = [
    "cgpa",
    "attendance",
    "study_hours_per_week",
    "assignments_completed",
    "previous_test_score",
    "total_questions_attempted",
    "total_correct",
    "total_incorrect",
    "accuracy",
    "semester",
    "streak_days",
    "participation_score",
]

FEATURE_COLUMNS = [
    "cgpa",
    "attendance",
    "study_hours_per_week",
    "assignments_completed",
    "previous_test_score",
    "total_questions_attempted",
    "total_correct",
    "total_incorrect",
    "accuracy",
    "semester",
    "streak_days",
    "participation_score",
]


class DataPreprocessor:
    """Clean, impute, scale and encode input data."""

    def __init__(self):
        self.imputer = SimpleImputer(strategy="mean")
        self.scaler = StandardScaler()
        self.grade_encoder = LabelEncoder()
        self._grade_classes = None

    def clean(self, df: pd.DataFrame) -> pd.DataFrame:
        """Data cleaning: drop useless columns, fix dtypes, clip ranges."""
        cleaned = df.copy()
        # Convert numeric
        for col in NUMERIC_FEATURES:
            if col in cleaned.columns:
                cleaned[col] = pd.to_numeric(cleaned[col], errors="coerce")
        # Clip ranges
        if "cgpa" in cleaned.columns:
            cleaned["cgpa"] = cleaned["cgpa"].clip(0, 10)
        for col in ["attendance", "assignments_completed", "previous_test_score",
                    "accuracy", "participation_score"]:
            if col in cleaned.columns:
                cleaned[col] = cleaned[col].clip(0, 100)
        if "study_hours_per_week" in cleaned.columns:
            cleaned["study_hours_per_week"] = cleaned["study_hours_per_week"].clip(0, 60)
        return cleaned

    def impute(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handle missing values with mean imputation."""
        imputed = df.copy()
        missing = [c for c in FEATURE_COLUMNS if c in imputed.columns]
        if missing:
            imputed[missing] = self.imputer.transform(imputed[missing])
        return imputed

    def fit(self, df: pd.DataFrame, grades: pd.Series):
        """Fit imputer, scaler and label encoder on training data."""
        cleaned = self.clean(df)
        cols = [c for c in FEATURE_COLUMNS if c in cleaned.columns]
        if cols:
            self.imputer.fit(cleaned[cols])
            cleaned[cols] = self.imputer.transform(cleaned[cols])
            self.scaler.fit(cleaned[cols])
        self.grade_encoder.fit(grades.astype(str))
        self._grade_classes = list(self.grade_encoder.classes_)

    def transform(self, df: pd.DataFrame) -> np.ndarray:
        """Clean, impute and scale input; return matrix aligned to FEATURE_COLUMNS."""
        cleaned = self.clean(df)
        cols = [c for c in FEATURE_COLUMNS if c in cleaned.columns]
        for c in FEATURE_COLUMNS:
            if c not in cleaned.columns:
                cleaned[c] = 0.0
        cleaned[FEATURE_COLUMNS] = self.imputer.transform(cleaned[FEATURE_COLUMNS])
        return self.scaler.transform(cleaned[FEATURE_COLUMNS])

    def encode_grade(self, grade: str) -> int:
        return int(self.grade_encoder.transform([grade])[0])

    def decode_grade(self, encoded: int) -> str:
        return self.grade_encoder.inverse_transform([encoded])[0]

    @property
    def grade_classes(self):
        return self._grade_classes


def map_score_to_grade(score: float) -> str:
    """Map a 0-100 score to a letter grade."""
    if score >= 90:
        return "A+"
    if score >= 80:
        return "A"
    if score >= 70:
        return "B+"
    if score >= 60:
        return "B"
    if score >= 50:
        return "C"
    if score >= 40:
        return "D"
    return "F"


def map_grade_to_score(grade: str) -> float:
    mapping = {"A+": 95, "A": 85, "B+": 75, "B": 65, "C": 55, "D": 45, "F": 35}
    return mapping.get(grade.upper(), 50)


def map_score_to_risk(score: float) -> str:
    if score >= 75:
        return "LOW"
    if score >= 60:
        return "MEDIUM"
    if score >= 45:
        return "HIGH"
    return "CRITICAL"
