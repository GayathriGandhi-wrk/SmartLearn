"""
Recommendation engine and study planner.

  - Weak subject detection from accuracy per subject
  - Knowledge gap detection from accuracy per topic
  - Content recommendations (videos, articles, questions, materials, revision)
  - AI study plan generation (daily / weekly / monthly / exam)
"""

import logging
from datetime import date, timedelta

logger = logging.getLogger("recommendation")

RECOMMENDATION_TEMPLATES = [
    {
        "type": "STUDY_MATERIAL",
        "resource_type": "MATERIAL",
        "title": "Strengthen {subject}",
        "description": "Focus on {subject}. Your accuracy is {accuracy}%. Revise the fundamentals first.",
        "url": "https://www.geeksforgeeks.org/search?q={query}",
        "priority": 1,
    },
    {
        "type": "PRACTICE",
        "resource_type": "QUESTION",
        "title": "Practice - {subject}",
        "description": "Attempt at least 20 MCQs on {subject} to raise accuracy above 70%.",
        "url": "/questions",
        "priority": 2,
    },
    {
        "type": "VIDEO",
        "resource_type": "VIDEO",
        "title": "Video Lectures - {topic}",
        "description": "Watch conceptual videos on {topic} to build strong fundamentals.",
        "url": "https://www.youtube.com/results?search_query={query}",
        "priority": 3,
    },
    {
        "type": "REVISION",
        "resource_type": "REVISION",
        "title": "Revise - {topic}",
        "description": "Knowledge gap in {topic} (mastery {mastery}%). Allocate {hours} hours.",
        "url": "/planner",
        "priority": 1,
    },
    {
        "type": "ARTICLE",
        "resource_type": "ARTICLE",
        "title": "Read about {topic}",
        "description": "Read a concise article to clarify doubts in {topic}.",
        "url": "https://www.geeksforgeeks.org/search?q={query}",
        "priority": 3,
    },
]


class RecommendationEngine:
    """Generates personalized recommendations from performance context."""

    def generate(self, context: dict) -> dict:
        recommendations = []
        weak_subjects = context.get("weak_subjects", [])
        knowledge_gaps = context.get("knowledge_gaps", [])

        for ws in weak_subjects[:3]:
            subject = ws.get("subject_name", "the subject")
            accuracy = ws.get("accuracy", 0)
            recommendations.append(self._fill(
                RECOMMENDATION_TEMPLATES[0],
                subject=subject, accuracy=accuracy, query=subject))
            recommendations.append(self._fill(
                RECOMMENDATION_TEMPLATES[1],
                subject=subject, accuracy=accuracy, query=subject))

        for gap in knowledge_gaps[:3]:
            topic = gap.get("topic_name", "the topic")
            mastery = gap.get("mastery_level", 0)
            hours = gap.get("recommended_hours", 3)
            recommendations.append(self._fill(
                RECOMMENDATION_TEMPLATES[3],
                topic=topic, mastery=mastery, hours=hours, query=topic))
            recommendations.append(self._fill(
                RECOMMENDATION_TEMPLATES[4],
                topic=topic, mastery=mastery, hours=hours, query=topic))

        if context.get("predicted_grade") in ("F", "D"):
            recommendations.append({
                "type": "REVISION",
                "resource_type": "REVISION",
                "title": "Intensive Revision Plan",
                "description": "Your predicted grade is low. Create a daily revision routine and attempt adaptive tests.",
                "url": "/planner",
                "priority": 1,
                "reason": "Low predicted performance",
            })

        return {"recommendations": recommendations}

    def _fill(self, template: dict, **kwargs) -> dict:
        item = {
            "type": template["type"],
            "resource_type": template["resource_type"],
            "title": template["title"].format(**kwargs),
            "description": template["description"].format(**kwargs),
            "url": template["url"].format(query=kwargs.get("query", "")),
            "priority": template["priority"],
        }
        if "mastery" in kwargs:
            item["reason"] = f"Knowledge gap level: {'HIGH' if kwargs['mastery'] < 40 else 'MEDIUM'}"
        return item


class StudyPlanner:
    """Builds structured daily/weekly/monthly/exam study plans."""

    def generate(self, context: dict, plan_type: str = "WEEKLY", total_hours: int = 20) -> dict:
        plan_type = (plan_type or "WEEKLY").upper()
        gaps = context.get("knowledge_gaps", [])
        topics = [g["topic_name"] for g in gaps[:5]] or ["General Revision"]

        days = {"DAILY": 1, "WEEKLY": 7, "MONTHLY": 30, "EXAM": max(7, total_hours // 4)}.get(plan_type, 7)
        hours_per_day = max(1, total_hours // max(days, 1))

        tasks = []
        start = date.today()
        for i in range(days):
            topic = topics[i % len(topics)]
            tasks.append({
                "day": f"Day {i + 1}",
                "date": (start + timedelta(days=i)).isoformat(),
                "topic": topic,
                "hours": hours_per_day,
                "action": f"Revise {topic} and attempt practice MCQs",
            })

        return {
            "plan_type": plan_type,
            "start_date": start.isoformat(),
            "end_date": (start + timedelta(days=days - 1)).isoformat(),
            "total_hours": total_hours,
            "tasks": tasks,
        }
