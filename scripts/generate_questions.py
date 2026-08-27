# -*- coding: utf-8 -*-
"""
Question Bank SQL Generator for the AI-Powered Adaptive Learning Platform.

Reads the taxonomy from question_catalog.py and the hand-authored questions
from scripts/question_data/subject_XX.py, then emits the 8 category seed files:

    database/seed_programming_questions.sql
    database/seed_database_questions.sql
    database/seed_network_questions.sql
    database/seed_os_questions.sql
    database/seed_ai_questions.sql
    database/seed_web_questions.sql
    database/seed_cloud_questions.sql
    database/seed_corecs_questions.sql

Each topic must contain exactly 10 BEGINNER + 10 INTERMEDIATE + 10 ADVANCED
questions (30 total). Marks: BEGINNER=1, INTERMEDIATE=2, ADVANCED=3.

Usage:
    python scripts/generate_questions.py            # write SQL files
    python scripts/generate_questions.py --check    # validate data only

Question data file format (scripts/question_data/subject_01.py):

    QUESTIONS = {
        # topic_id -> [ (difficulty, question, [oA, oB, oC, oD], correct_letter, explanation), ... ]
        1: [
            ("BEGINNER", "What does the % operator return in C?",
             ["Product", "Remainder", "Quotient", "Sum"], "B",
             "The % operator returns the remainder after integer division, e.g. 7 % 3 = 1."),
        ],
    }
"""

import importlib.util
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))   # AI-Student-Performance-System
sys.path.insert(0, os.path.join(ROOT, "scripts"))
sys.path.insert(0, os.path.join(ROOT, "scripts", "question_data"))

import question_catalog as catalog

DATA_DIR = os.path.join(ROOT, "scripts", "question_data")
DB_DIR = os.path.join(ROOT, "database")

DIFFICULTIES = ("BEGINNER", "INTERMEDIATE", "ADVANCED")
MARKS = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}

# category_id -> output file stem
CATEGORY_FILES = {
    1: "seed_programming_questions.sql",
    2: "seed_database_questions.sql",
    3: "seed_network_questions.sql",
    4: "seed_os_questions.sql",
    5: "seed_ai_questions.sql",
    6: "seed_web_questions.sql",
    7: "seed_cloud_questions.sql",
    8: "seed_corecs_questions.sql",
}


def load_data_files():
    """Import every subject_XX.py file in question_data/ and merge QUESTIONS."""
    if not os.path.isdir(DATA_DIR):
        raise SystemExit(f"question data directory not found: {DATA_DIR}")
    merged = {}
    loaded = []
    for fn in sorted(os.listdir(DATA_DIR)):
        if not re.fullmatch(r"subject_\d{2}\.py", fn):
            continue
        path = os.path.join(DATA_DIR, fn)
        mod_name = os.path.splitext(fn)[0]
        spec = importlib.util.spec_from_file_location(mod_name, path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        q = getattr(mod, "QUESTIONS", None)
        if not isinstance(q, dict):
            raise SystemExit(f"{fn}: missing QUESTIONS dict")
        merged.update(q)
        loaded.append(fn)
    if not loaded:
        raise SystemExit("no subject_XX.py data files found")
    return merged


def validate(questions):
    """Return (errors, warnings, per_topic_summary)."""
    errors = []
    warnings = []

    catalog_ids = {t[0] for t in catalog.TOPICS}
    for tid in questions:
        if tid not in catalog_ids:
            errors.append(f"topic_id {tid} not present in catalog")

    for tid in sorted(catalog_ids):
        rows = questions.get(tid, [])
        name = dict((t[0], t[2]) for t in catalog.TOPICS).get(tid, "?")
        if len(rows) != 30:
            errors.append(f"topic {tid} ({name}): {len(rows)} questions (expected 30)")
            continue
        counts = {d: 0 for d in DIFFICULTIES}
        for row in rows:
            diff = row[0]
            if diff not in DIFFICULTIES:
                errors.append(f"topic {tid}: invalid difficulty {diff!r}")
            else:
                counts[diff] += 1
            qtext = row[1]
            if not qtext or not isinstance(qtext, str):
                errors.append(f"topic {tid}: empty question text")
            opts = row[2]
            if not isinstance(opts, (list, tuple)) or len(opts) != 4:
                errors.append(f"topic {tid}: question has {len(opts)} options: {qtext[:50]}")
            if row[3] not in ("A", "B", "C", "D"):
                errors.append(f"topic {tid}: invalid correct answer {row[3]!r}: {qtext[:50]}")
            expl = row[4]
            if not expl or not isinstance(expl, str):
                warnings.append(f"topic {tid}: missing explanation: {qtext[:50]}")
        for d, c in counts.items():
            if c != 10:
                errors.append(f"topic {tid} ({name}): {d} has {c} (expected 10)")

    # global duplicate detection on normalized text
    seen = {}
    for tid in sorted(catalog_ids):
        for row in questions.get(tid, []):
            norm = re.sub(r"\s+", " ", row[1].strip().lower())
            if norm in seen:
                errors.append(f"duplicate question: '{row[1][:60]}...' (topics {seen[norm]} and {tid})")
            else:
                seen[norm] = tid
    return errors, warnings


def sql_str(value):
    """Escape a string for a single-quoted SQL literal."""
    s = str(value)
    s = s.replace("\\", "\\\\").replace("'", "''").replace("\r", " ").replace("\n", " ").replace("\t", " ")
    return "'" + s + "'"


def emit_category(cid, questions, start_qid):
    """Write one category seed file. Returns (qid_after, total)."""
    topics = catalog.topics_of_category(cid)
    path = os.path.join(DB_DIR, CATEGORY_FILES[cid])
    cat_name = catalog.category_by_id(cid)[1]
    count = 0
    qid = start_qid
    chunks = []
    current = []

    for topic in topics:
        tid = topic[0]
        for row in questions[tid]:
            diff, qtext, opts, correct, expl = row
            current.append(
                "({0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}, {9}, {10}, 'MCQ', 1)".format(
                    qid,
                    tid,
                    sql_str(qtext),
                    sql_str(opts[0]),
                    sql_str(opts[1]),
                    sql_str(opts[2]),
                    sql_str(opts[3]),
                    sql_str(correct),
                    sql_str(expl),
                    sql_str(diff),
                    MARKS[diff],
                )
            )
            qid += 1
            count += 1
            if len(current) >= 250:
                chunks.append(current)
                current = []
    if current:
        chunks.append(current)

    header = (
        "-- ============================================================================\n"
        "-- AI-Powered Student Performance Prediction & Personalized Learning System\n"
        f"-- CATEGORY {cid}: {cat_name}\n"
        f"-- Question seed file (auto-generated by scripts/generate_questions.py)\n"
        f"-- {count} MCQs | topic_id ranges: {topics[0][0]}-{topics[-1][0]}\n"
        "-- ============================================================================\n\n"
        "USE student_performance_db;\n\n"
    )
    lines = [header]
    for chunk in chunks:
        lines.append(
            "INSERT INTO questions (question_id, topic_id, question_text, option_a, option_b, option_c, option_d, correct_answer, explanation, difficulty, marks, question_type, is_active) VALUES\n"
            + ",\n".join(chunk)
            + ";\n"
        )
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("".join(lines))
    return qid, count


def check_base_sql():
    """Verify seed_base.sql subjects/topics match the catalog."""
    base_path = os.path.join(DB_DIR, "seed_base.sql")
    with open(base_path, encoding="utf-8") as f:
        text = f.read()

    subj_m = re.search(r"INSERT INTO subjects .*?VALUES(.*?);", text, re.S)
    subj_sql = subj_m.group(1) if subj_m else ""
    topics_m = re.search(r"INSERT INTO topics .*?VALUES(.*?);", text, re.S)
    topics_sql = topics_m.group(1) if topics_m else ""

    parsed_subjects = {int(m[0]): m[3] for m in re.findall(r"\((\d+),\s*(\d+),\s*'([^']+)',\s*'([^']+)'", subj_sql)}
    parsed_topics = {int(m[0]): m[2] for m in re.findall(r"\((\d+),\s*(\d+),\s*'([^']+)'", topics_sql)}

    problems = []
    for sid, _, _, sname, *_ in catalog.SUBJECTS:
        if sid not in parsed_subjects:
            problems.append(f"subject {sid} missing in seed_base.sql")
        elif parsed_subjects[sid] != sname:
            problems.append(f"subject {sid}: seed_base '{parsed_subjects[sid]}' != catalog '{sname}'")
    for tid, _, tname, *_ in catalog.TOPICS:
        if tid not in parsed_topics:
            problems.append(f"topic {tid} missing in seed_base.sql")
        elif parsed_topics[tid] != tname:
            problems.append(f"topic {tid}: seed_base '{parsed_topics[tid]}' != catalog '{tname}'")
    return problems


def main():
    check_only = "--check" in sys.argv
    problems = check_base_sql()
    if problems:
        raise SystemExit("seed_base.sql mismatch:\n  " + "\n  ".join(problems))

    questions = load_data_files()
    errors, warnings = validate(questions)
    if warnings:
        print("[warn] " + "\n[warn] ".join(warnings))
    if errors:
        raise SystemExit("VALIDATION FAILED:\n  " + "\n  ".join(errors[:80]))

    if check_only:
        print("validation OK: 200 topics x 30 questions")
        return

    qid = 1
    total = 0
    for cid in sorted(CATEGORY_FILES):
        qid, cnt = emit_category(cid, questions, qid)
        total += cnt
        print(f"cat {cid}: {CATEGORY_FILES[cid]} -> {cnt} questions")

    print(f"\nDONE. {total} questions written across {len(CATEGORY_FILES)} files.")


if __name__ == "__main__":
    main()
