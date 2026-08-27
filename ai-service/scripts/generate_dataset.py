"""
Synthetic student performance dataset generator.
Run: python scripts/generate_dataset.py
Output: ai-service/data/student_performance.csv
"""

import os
import numpy as np
import pandas as pd

from app.preprocessing import map_score_to_grade

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "data", "student_performance.csv")


def generate(n=5000, seed=42):
    rng = np.random.default_rng(seed)
    cgpa = rng.uniform(4.0, 9.8, n)
    attendance = rng.uniform(50, 100, n)
    study_hours = rng.uniform(1, 15, n)
    assignments = rng.uniform(35, 100, n)
    prev_score = rng.uniform(30, 100, n)
    attempted = rng.integers(5, 300, n).astype(float)
    accuracy = rng.uniform(25, 100, n)
    semester = rng.integers(1, 9, n).astype(float)
    streak = rng.integers(0, 40, n).astype(float)
    participation = rng.uniform(25, 100, n)

    base = (0.45 * (cgpa / 10) * 100 + 0.15 * attendance
            + 0.10 * np.clip(study_hours * 8, 0, 100) + 0.08 * assignments
            + 0.07 * prev_score + 0.10 * accuracy + 0.05 * participation)
    score = np.clip(base + rng.normal(0, 6, n), 20, 100)

    df = pd.DataFrame({
        "cgpa": cgpa.round(2),
        "attendance": attendance.round(2),
        "study_hours_per_week": study_hours.round(1),
        "assignments_completed": assignments.round(2),
        "previous_test_score": prev_score.round(2),
        "total_questions_attempted": attempted,
        "total_correct": (attempted * accuracy / 100).round().astype(int),
        "total_incorrect": (attempted * (1 - accuracy / 100)).round().astype(int),
        "accuracy": accuracy.round(2),
        "semester": semester,
        "streak_days": streak,
        "participation_score": participation.round(2),
        "score": score.round(2),
        "grade": [map_score_to_grade(s) for s in score],
    })
    return df


if __name__ == "__main__":
    import sys
    sys.path.insert(0, ROOT)
    df = generate()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    df.to_csv(OUT, index=False)
    print(f"Generated {len(df)} rows -> {OUT}")
