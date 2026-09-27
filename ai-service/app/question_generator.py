"""
Topic-based question generator.

Generates fresh MCQs for a topic using Gemini when an API key is configured,
and falls back to deterministic templates so the feature still works offline.
Callers pass the question texts a student has already been served so the same
question is never generated twice for that student.
"""

import json
import logging
import os
import re

logger = logging.getLogger("question_generator")

DIFFICULTIES = ("BEGINNER", "INTERMEDIATE", "ADVANCED")

# Question shapes used by the offline fallback. Each entry is
# (question template, correct answer template, three distractor templates).
FALLBACK_TEMPLATES = [
    (
        "Which statement best describes {topic}?",
        "{topic} is a core concept in {subject} that students must understand to work with it effectively.",
        [
            "{topic} is an unrelated topic with no connection to {subject}.",
            "{topic} is only used in industries outside {subject}.",
            "{topic} has no effect on how {subject} systems behave.",
        ],
    ),
    (
        "In {subject}, what is the primary purpose of {topic}?",
        "To provide the techniques and principles needed to solve problems in {subject}.",
        [
            "To increase the hardware cost of a {subject} project.",
            "To replace the need for testing in {subject}.",
            "To reduce the number of topics studied in {subject}.",
        ],
    ),
    (
        "Which skill is most useful when applying {topic}?",
        "Breaking the problem down and reasoning about it step by step.",
        [
            "Memorising definitions without practising them.",
            "Avoiding all written work.",
            "Skipping verification of results.",
        ],
    ),
    (
        "A student is stuck on a problem involving {topic}. What should they do first?",
        "Review the definitions and work through a simple example of {topic}.",
        [
            "Skip the topic and revise an unrelated chapter.",
            "Copy an answer without understanding it.",
            "Give up on the problem permanently.",
        ],
    ),
    (
        "Which statement about {topic} is TRUE?",
        "{topic} builds on the fundamental ideas of {subject} and extends them to practical problems.",
        [
            "{topic} contradicts every other topic in {subject}.",
            "{topic} is only relevant to theoretical research.",
            "{topic} has no connection to {subject} at all.",
        ],
    ),
    (
        "How deeply should a student master {topic}?",
        "Enough to explain the concept and apply it to unfamiliar problems.",
        [
            "Memorising the wording of the definition only.",
            "Only knowing the name of the concept.",
            "No study at all, since it is never examined.",
        ],
    ),
    (
        "Which of the following is the best way to verify your understanding of {topic}?",
        "Solve practice problems on {topic} and review the explanations for mistakes.",
        [
            "Reading the heading once and moving on.",
            "Assuming the topic is clear because it sounds familiar.",
            "Waiting for an exam to find out what was missed.",
        ],
    ),
    (
        "In an exam, a question on {topic} is likely to test:",
        "Your ability to apply the concept of {topic} to a new situation.",
        [
            "How quickly you can write without reading the question.",
            "How many unrelated topics you have memorised.",
            "Whether you skipped the topic entirely.",
        ],
    ),
    (
        "Why is {topic} taught as part of {subject}?",
        "Later topics and real problems in {subject} are built on it, so it cannot be skipped.",
        [
            "It is only included to fill the syllabus length.",
            "It is unrelated to any practical work in {subject}.",
            "It is easier than every other topic in {subject}.",
        ],
    ),
    (
        "A classmate explains {topic} in their own words. What shows real understanding?",
        "They can explain the idea clearly and then apply it to a problem that was not in the notes.",
        [
            "They repeat the definition word for word and stop there.",
            "They use the longest words they can find.",
            "They say the topic is easy without giving a reason.",
        ],
    ),
    (
        "Which of these is a realistic use of {topic} in industry?",
        "Solving a real constraint in a {subject} system where the standard approach is insufficient.",
        [
            "Avoiding all engineering work in {subject}.",
            "Replacing every design decision with guesswork.",
            "Declining to test the solution before shipping it.",
        ],
    ),
    (
        "What is the most common mistake students make with {topic}?",
        "Memorising the steps without understanding why each step is needed.",
        [
            "Writing the answer in a readable handwriting.",
            "Checking the result against the expected outcome.",
            "Asking for help when a step is unclear.",
        ],
    ),
    (
        "How would you revise {topic} the night before a test?",
        "Work through several varied problems on {topic} and note where you needed the notes.",
        [
            "Read the chapter once and stop.",
            "Study a completely different topic instead.",
            "Only revise the parts you already know well.",
        ],
    ),
    (
        "A practical problem needs {topic}. What must you identify first?",
        "The goal of the problem, so you can decide which parts of {topic} are actually needed.",
        [
            "The shortest possible answer, before understanding the problem.",
            "The code style to use, ignoring the goal.",
            "Whether someone else has already solved it.",
        ],
    ),
    (
        "Which statement shows a student has moved past basic {topic} knowledge?",
        "They can adapt {topic} to a problem that is not a direct copy of an example.",
        [
            "They can recite the definition of {topic} word for word.",
            "They have completed only the introduction page.",
            "They avoid using {topic} in any exercise.",
        ],
    ),
]

SYSTEM_PROMPT = (
    "You write exam-quality multiple choice questions for engineering students. "
    "Return ONLY a JSON array. Every element must be an object with exactly these keys: "
    '"question_text" (string), "option_a", "option_b", "option_c", "option_d" (strings), '
    '"correct_answer" (one of "A", "B", "C", "D"), "explanation" (string), '
    '"difficulty" (one of "BEGINNER", "INTERMEDIATE", "ADVANCED"). '
    "Exactly one option must be correct. Keep options concise and of similar length. "
    "Do not number the questions and do not wrap the JSON in markdown fences."
)


def normalize(text) -> str:
    """Lowercase and collapse punctuation/whitespace so near-duplicates collide."""
    if not text:
        return ""
    cleaned = re.sub(r"[^a-z0-9 ]+", " ", str(text).lower())
    return re.sub(r"\s+", " ", cleaned).strip()


class QuestionGeneratorService:
    """Generates topic questions via Gemini, or templates when unavailable."""

    def __init__(self):
        self.provider = os.environ.get("QUESTION_PROVIDER", "gemini").lower()
        self.gemini_key = os.environ.get("GEMINI_API_KEY", "")
        self.openai_key = os.environ.get("OPENAI_API_KEY", "")
        self.model = os.environ.get("QUESTION_MODEL", "gemini-1.5-flash")
        self.openai_model = os.environ.get("QUESTION_OPENAI_MODEL", "gpt-4o-mini")

    # ------------------------------------------------------------------ #
    def _has_real_key(self, key: str) -> bool:
        return bool(key) and "your-" not in key and "here" not in key

    def generate(self, topic: str, subject: str = "", resource_title: str = "",
                 difficulty: str = "BEGINNER", count: int = 5,
                 avoid: list = None, provider: str = None) -> dict:
        topic = (topic or "this topic").strip()
        subject = (subject or "").strip()
        resource_title = (resource_title or "").strip()
        difficulty = (difficulty or "BEGINNER").upper()
        if difficulty not in DIFFICULTIES:
            difficulty = "BEGINNER"
        count = max(1, min(int(count or 5), 10))
        avoid_norm = {normalize(a) for a in (avoid or []) if a}

        provider = (provider or self.provider).lower()
        questions = []
        used_provider = "template"

        if provider in ("gemini", "openai") and self._try_ai(provider, topic, subject,
                                                             resource_title, difficulty,
                                                             count, avoid_norm):
            questions = self._last_result
            used_provider = provider

        if len(questions) < count:
            questions.extend(self._fallback(topic, subject, difficulty,
                                            count - len(questions), avoid_norm))
            if questions and used_provider == provider:
                used_provider = f"{provider}+template"

        unique = self._dedupe(questions, avoid_norm)
        return {
            "questions": unique[:count],
            "provider": used_provider,
            "available": bool(questions),
            "topic": topic,
            "difficulty": difficulty,
        }

    # ------------------------------------------------------------------ #
    def _try_ai(self, provider: str, topic: str, subject: str, resource_title: str,
                difficulty: str, count: int, avoid_norm: set) -> bool:
        key = self.gemini_key if provider == "gemini" else self.openai_key
        if not self._has_real_key(key):
            logger.info("%s key not configured for question generation; using templates.", provider.upper())
            return False

        avoid_hint = ""
        if avoid_norm:
            sample = list(avoid_norm)[:12]
            avoid_hint = ("\n\nDo NOT reuse or lightly rephrase any of these questions "
                          "already given to this student:\n- " + "\n- ".join(sample))

        focus = f"Subject: {subject}\n" if subject else ""
        focus += f"Topic: {topic}\n"
        if resource_title:
            focus += f'The student just finished this resource: "{resource_title}".\n'
        focus += f"Difficulty: {difficulty}\nProduce {count} distinct questions."
        prompt = f"{focus}{avoid_hint}"

        try:
            if provider == "gemini":
                self._last_result = self._gemini(prompt)
            else:
                self._last_result = self._openai(prompt)
            return bool(self._last_result)
        except Exception as exc:
            logger.warning("Question generation via %s failed: %s", provider, exc)
            return False

    def _gemini(self, prompt: str) -> list:
        from google import genai
        client = genai.Client(api_key=self.gemini_key)
        response = client.models.generate_content(
            model=self.model,
            contents=f"{SYSTEM_PROMPT}\n\n{prompt}",
            config={"response_mime_type": "application/json"},
        )
        return self._parse(response.text)

    def _openai(self, prompt: str) -> list:
        from openai import OpenAI
        client = OpenAI(api_key=self.openai_key)
        response = client.chat.completions.create(
            model=self.openai_model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            temperature=1.0,
            max_tokens=2000,
            response_format={"type": "json_object"},
        )
        return self._parse(response.response_format and response.choices[0].message.content)

    # ------------------------------------------------------------------ #
    def _parse(self, raw: str) -> list:
        """Parse and validate whatever the model returned into clean MCQs."""
        if not raw:
            return []
        text = raw.strip()
        if text.startswith("```"):
            text = re.sub(r"^```[a-zA-Z]*\s*", "", text)
            text = re.sub(r"```$", "", text).strip()
        try:
            data = json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\[.*\]", text, re.S)
            if not match:
                logger.warning("Could not parse generated questions as JSON")
                return []
            try:
                data = json.loads(match.group(0))
            except json.JSONDecodeError:
                return []

        if isinstance(data, dict):
            data = data.get("questions") or data.get("data") or []
        if not isinstance(data, list):
            return []

        cleaned = []
        for row in data:
            item = self._validate(row)
            if item:
                cleaned.append(item)
        return cleaned

    def _validate(self, row) -> dict:
        if not isinstance(row, dict):
            return None
        try:
            text = str(row.get("question_text") or row.get("question") or "").strip()
            options = {k: str(row.get(f"option_{k.lower()}") or "").strip()
                       for k in ("A", "B", "C", "D")}
            answer = str(row.get("correct_answer") or "").strip().upper()[:1]
        except Exception:
            return None

        if not text or answer not in ("A", "B", "C", "D"):
            return None
        if not all(options.values()):
            return None
        if len({v.lower() for v in options.values()}) != 4:
            return None
        if not options[answer]:
            return None

        difficulty = str(row.get("difficulty") or "BEGINNER").strip().upper()
        if difficulty not in DIFFICULTIES:
            difficulty = "BEGINNER"

        return {
            "question_text": text[:1000],
            "option_a": options["A"][:500],
            "option_b": options["B"][:500],
            "option_c": options["C"][:500],
            "option_d": options["D"][:500],
            "correct_answer": answer,
            "explanation": str(row.get("explanation") or "").strip()[:1000],
            "difficulty": difficulty,
        }

    # ------------------------------------------------------------------ #
    def _fallback(self, topic: str, subject: str, difficulty: str,
                  needed: int, avoid_norm: set) -> list:
        """Deterministic template questions, rotated so repeats are unlikely."""
        if needed <= 0:
            return []
        subject = subject or "the subject"
        out = []
        span = len(FALLBACK_TEMPLATES)
        offset = sum(ord(c) for c in (topic + difficulty)) % span
        for i in range(span * 3):
            if len(out) >= needed:
                break
            question_t, correct_t, wrong_t = FALLBACK_TEMPLATES[(offset + i) % span]
            question = question_t.format(topic=topic, subject=subject)
            if normalize(question) in avoid_norm:
                continue
            correct = correct_t.format(topic=topic, subject=subject)
            wrongs = [w.format(topic=topic, subject=subject) for w in wrong_t]
            for wrong in wrongs:
                if normalize(wrong) == normalize(correct):
                    return out
            # Rotate the correct answer so it is not always A.
            shift = (offset + i) % 4
            order = [correct] + wrongs
            options = order[-shift:] + order[:-shift] if shift else order
            letters = ("A", "B", "C", "D")
            payload = {"question_text": question, "explanation": correct}
            for letter, value in zip(letters, options):
                payload[f"option_{letter.lower()}"] = value
            payload["correct_answer"] = letters[options.index(correct)]
            payload["difficulty"] = difficulty
            out.append(payload)
        return out

    def _dedupe(self, questions: list, avoid_norm: set) -> list:
        seen = set()
        out = []
        for q in questions:
            key = normalize(q.get("question_text"))
            if not key or key in seen or key in avoid_norm:
                continue
            seen.add(key)
            out.append(q)
        return out
