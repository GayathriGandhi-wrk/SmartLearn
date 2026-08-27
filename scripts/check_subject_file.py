# -*- coding: utf-8 -*-
"""
Self-check helper for a single question data file.

Usage:  python scripts/check_subject_file.py scripts/question_data/subject_01.py

Prints per-topic totals and difficulty counts and reports any violation of the
10 BEGINNER / 10 INTERMEDIATE / 10 ADVANCED rule.
"""

import importlib.util
import re
import sys
from collections import Counter

ALLOWED = ("BEGINNER", "INTERMEDIATE", "ADVANCED")


def main(path):
    spec = importlib.util.spec_from_file_location("chk", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    q = mod.QUESTIONS

    print(f"{path}: {len(q)} topics")
    problems = 0
    for tid in sorted(q):
        rows = q[tid]
        counts = Counter(r[0] for r in rows)
        print(f"  topic {tid}: {len(rows)} questions -> {dict(counts)}")
        if len(rows) != 30:
            problems += 1
        for d in ALLOWED:
            if counts.get(d, 0) != 10:
                problems += 1
        for r in rows:
            if r[0] not in ALLOWED:
                problems += 1
            if len(r[2]) != 4:
                problems += 1
            if r[3] not in ("A", "B", "C", "D"):
                problems += 1
    # duplicate check
    seen = set()
    for tid in q:
        for r in q[tid]:
            norm = re.sub(r"\s+", " ", r[1].strip().lower())
            if norm in seen:
                print(f"  DUPLICATE question in topic {tid}: {r[1][:60]}")
                problems += 1
            seen.add(norm)
    print("RESULT:", "OK - 150 questions, counts valid" if problems == 0 else f"PROBLEMS: {problems}")
    return problems


if __name__ == "__main__":
    sys.exit(1 if main(sys.argv[1]) else 0)
