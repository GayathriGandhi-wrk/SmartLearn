import os
import sys

import pytest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.preprocessing import DataPreprocessor, map_score_to_grade, map_score_to_risk
from app.models import ModelManager


@pytest.fixture(scope="module")
def model_manager(tmp_path_factory):
    model_dir = str(tmp_path_factory.mktemp("models"))
    mm = ModelManager(model_dir=model_dir)
    pp = DataPreprocessor()
    df = mm.generate_synthetic_dataset(n=400)
    mm.train(df, pp)
    return mm


def test_grade_mapping():
    assert map_score_to_grade(95) == "A+"
    assert map_score_to_grade(65) == "B"
    assert map_score_to_grade(35) == "F"
    assert map_score_to_risk(80) == "LOW"
    assert map_score_to_risk(50) == "HIGH"


def test_preprocessor_roundtrip(model_manager):
    pp = model_manager.preprocessor
    sample = {
        "cgpa": 8.5, "attendance": 90, "study_hours_per_week": 6,
        "assignments_completed": 85, "previous_test_score": 78,
        "total_questions_attempted": 120, "total_correct": 90,
        "total_incorrect": 30, "accuracy": 75, "semester": 5,
        "streak_days": 6, "participation_score": 80,
    }
    X = pp.transform(__import__("pandas").DataFrame([sample]))
    assert X.shape == (1, 12)
    assert X[0][0] != 0  # scaled value


def test_predictions_are_in_range(model_manager):
    sample = {
        "cgpa": 8.0, "attendance": 88, "study_hours_per_week": 7,
        "assignments_completed": 80, "previous_test_score": 75,
        "total_questions_attempted": 150, "total_correct": 110,
        "total_incorrect": 40, "accuracy": 73, "semester": 5,
        "streak_days": 10, "participation_score": 82,
    }
    result = model_manager.predict(sample)
    assert 0 <= result["predicted_score"] <= 100
    assert result["predicted_grade"] in {"A+", "A", "B+", "B", "C", "D", "F"}
    assert result["risk_level"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
    assert 50 <= result["confidence_score"] <= 99
    assert len(result["feature_importance"]) > 0


def test_all_models_trained(model_manager):
    assert set(model_manager.models.keys()) == {
        "random_forest", "gradient_boosting", "xgboost", "decision_tree"}
