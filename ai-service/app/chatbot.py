"""
AI Chatbot service.

Uses Gemini API or OpenAI API when configured; falls back to a
rule-based educational assistant otherwise so the system works offline.
"""

import logging
import os
import re

logger = logging.getLogger("chatbot")

CONCEPT_ANSWERS = {
    "binary search": ("Binary search is an O(log n) algorithm that repeatedly divides "
                      "a sorted array in half, comparing the target with the middle element "
                      "to decide which half to search next."),
    "normalization": ("Normalization organizes database tables to reduce redundancy and "
                      "dependency by dividing tables and defining relationships (1NF, 2NF, 3NF, BCNF)."),
    "linked list": ("A linked list stores elements in nodes, each pointing to the next node. "
                    "It supports O(1) insertion at known position but O(n) indexed access."),
    "stack": ("A stack is a LIFO data structure supporting push, pop and peek in O(1) time."),
    "queue": ("A queue is a FIFO data structure with enqueue at the rear and dequeue at the front."),
    "dijkstra": ("Dijkstra's algorithm finds shortest paths from a source node to all others "
                 "in a weighted graph with non-negative edges, running in O((V+E) log V) with a heap."),
    "dynamic programming": ("Dynamic programming solves problems by breaking them into "
                            "overlapping subproblems and storing results to avoid recomputation."),
    "operating system": ("An operating system manages hardware and software resources, "
                         "handling processes, memory, files and I/O."),
    "jvm": ("The JVM executes Java bytecode, managing memory, garbage collection and "
            "platform independence."),
}


class ChatbotService:
    """Handles chatbot requests via Gemini / OpenAI / rules."""

    def __init__(self):
        self.provider = os.environ.get("CHATBOT_PROVIDER", "rule").lower()
        self.gemini_key = os.environ.get("GEMINI_API_KEY", "")
        self.openai_key = os.environ.get("OPENAI_API_KEY", "")
        self.model = os.environ.get("CHATBOT_MODEL", "gemini-1.5-flash")
        self.openai_model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")

    def _has_real_key(self, key: str) -> bool:
        return bool(key) and "your-" not in key and "here" not in key

    def chat(self, message: str, provider: str = None, context: dict = None,
             history: list = None) -> dict:
        provider = (provider or self.provider).lower()
        if provider == "openai":
            if self._has_real_key(self.openai_key):
                try:
                    return self._openai(message, context, history)
                except Exception as exc:
                    logger.warning("Chatbot openai error: %s", exc)
            else:
                logger.warning("OPENAI_API_KEY is not configured. Using rule-based fallback.")
        elif provider == "gemini":
            if self._has_real_key(self.gemini_key):
                try:
                    return self._gemini(message, context)
                except Exception as exc:
                    logger.warning("Chatbot gemini error: %s", exc)
            else:
                logger.warning("GEMINI_API_KEY is not configured. Using rule-based fallback.")
        return self._rule_based(message, context)

    # ------------------------------------------------------------------ #
    def _rule_based(self, message: str, context: dict) -> dict:
        text = (message or "").lower()
        for keyword, answer in CONCEPT_ANSWERS.items():
            if keyword in text:
                return {"reply": answer, "intent": "CONCEPT_EXPLANATION",
                        "confidence": 0.92, "provider": "rule"}
        if any(w in text for w in ("hello", "hi", "hey")):
            return {"reply": "Hello! I'm your AI study assistant. Ask me about concepts, "
                             "study plans, doubts or your performance predictions.",
                    "intent": "GREETING", "confidence": 0.95, "provider": "rule"}
        if any(w in text for w in ("study", "plan", "schedule", "revision")):
            return {"reply": "Recommended routine: 1) Revise weakest topics first, "
                             "2) Attempt 15-20 practice MCQs daily, 3) Take one adaptive test per day, "
                             "4) Review wrong answers at night.",
                    "intent": "STUDY_GUIDANCE", "confidence": 0.9, "provider": "rule"}
        if any(w in text for w in ("predict", "grade", "score", "risk")):
            return {"reply": "Open the AI Prediction page to see your predicted grade and risk. "
                             "Improve attendance and daily practice to boost your score.",
                    "intent": "PERFORMANCE_INSIGHT", "confidence": 0.88, "provider": "rule"}
        if any(w in text for w in ("doubt", "confused", "not clear", "difficult")):
            return {"reply": "Let's clarify it step by step. Tell me the exact topic or question "
                             "and I'll break it down for you.",
                    "intent": "DOUBT_SOLVING", "confidence": 0.85, "provider": "rule"}
        return {"reply": "I can help with concepts, study planning, doubts and predictions. "
                         "Try asking about 'binary search', 'normalization' or 'study plan'.",
                "intent": "GENERAL", "confidence": 0.7, "provider": "rule"}

    # ------------------------------------------------------------------ #
    def _gemini(self, message: str, context: dict) -> dict:
        from google import genai
        client = genai.Client(api_key=self.gemini_key)
        system = ("You are an educational AI tutor for engineering students. "
                  "Answer clearly, concisely and helpfully. Context: "
                  + (str(context or {})[:500]))
        response = client.models.generate_content(
            model=self.model,
            contents=f"{system}\n\nStudent: {message}",
        )
        return {"reply": response.text, "intent": "AI",
                "confidence": 0.96, "provider": "gemini"}

    def _openai(self, message: str, context: dict, history: list = None) -> dict:
        from openai import OpenAI
        client = OpenAI(api_key=self.openai_key)
        context_block = ""
        if context:
            context_block = ("Relevant student context: "
                             + str(context)[:800] + "\n\n")
        system = ("You are an educational AI tutor for engineering students. "
                  "Answer clearly, concisely and helpfully. "
                  "Use the provided student context and conversation history when answering. "
                  "Do not repeat answers you have already given.")
        messages = [{"role": "system", "content": system}]
        if history:
            for turn in history[-10:]:
                role = "assistant" if str(turn.get("role", "")).lower() == "assistant" else "user"
                messages.append({"role": role, "content": str(turn.get("content", ""))})
        messages.append({"role": "user", "content": context_block + message})
        response = client.chat.completions.create(
            model=self.openai_model,
            messages=messages,
            temperature=0.7,
            max_tokens=500,
        )
        return {"reply": response.choices[0].message.content,
                "intent": "AI", "confidence": 0.96, "provider": "openai"}
