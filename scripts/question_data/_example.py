# -*- coding: utf-8 -*-
"""
FORMAT SPEC for question data files (subject_XX.py) in this directory.

The generator (scripts/generate_questions.py) loads every file matching
subject_XX.py and reads the QUESTIONS dict. Each file covers exactly ONE
subject = 5 topics x 30 questions = 150 questions.

RULE per topic: exactly 30 questions -> 10 BEGINNER, 10 INTERMEDIATE, 10 ADVANCED.

Each entry is a tuple:
    ("DIFFICULTY", "question text", ["option a", "option b", "option c", "option d"], "A", "explanation")

Constraints:
  - Difficulty is one of: BEGINNER, INTERMEDIATE, ADVANCED
  - Exactly 4 options per question
  - correct letter is one of: A, B, C, D and MUST match the correct option
  - No question_text may be repeated anywhere in the whole bank (keep them distinct)
  - Explanation must be a complete, informative sentence explaining WHY the answer is correct
  - Options must be plausible (wrong options are realistic distractors, not jokes)
  - Distribute correct answers across A/B/C/D (do not always use the same letter)
  - Use normal Python double-quoted strings. Avoid embedded double quotes; if you
    need quotes inside a string use single quotes ('). The generator escapes single
    quotes for SQL automatically.
  - Do NOT use triple-quoted strings or line breaks inside a tuple element.

Quality bar per difficulty:
  - BEGINNER  : definitions, syntax, basic concepts, easy MCQs
  - INTERMEDIATE: code tracing, scenario questions, concept application, problem solving
  - ADVANCED  : real-world scenarios, optimization, design, case-study style, interview-level

Example (topic 1 has a partial set shown below):
"""

QUESTIONS = {
    1: [
        ("BEGINNER", "Which keyword declares a single-precision floating point variable in C?",
         ["float", "double", "long", "int"], "A",
         "float is the C keyword for a single-precision floating-point type, typically 4 bytes."),
        ("BEGINNER", "What is the default return type of a C function when none is specified?",
         ["int", "void", "float", "char"], "A",
         "In older C standards an undeclared function return type defaults to int."),
        ("INTERMEDIATE", "Consider int x = 5, y = 2; what is the value of x / y in C?",
         ["2", "2.5", "3", "2.0"], "A",
         "Integer division truncates the fractional part, so 5 / 2 evaluates to 2."),
        ("ADVANCED", "A function returns a pointer to a local array. Why is this dangerous?",
         ["The array memory is freed when the function returns, leaving a dangling pointer",
          "Pointers cannot point to arrays",
          "The array is copied on return",
          "Returning pointers is a syntax error"], "A",
         "Local arrays live on the stack frame of the function; accessing them after return is undefined behaviour."),
    ],
}
