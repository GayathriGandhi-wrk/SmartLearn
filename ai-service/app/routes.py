"""
Flask REST API routes for the AI service.
"""

import logging

import pandas as pd
from flask import Blueprint, jsonify, request

from app.preprocessing import FEATURE_COLUMNS, map_score_to_grade, map_score_to_risk

logger = logging.getLogger("routes")


def _extract_features(payload: dict) -> dict:
    """Normalize the incoming payload into the feature set the models expect."""
    features = {k: v for k, v in (payload or {}).items() if k != "features"}
    if payload and "features" in payload and isinstance(payload["features"], dict):
        features.update(payload["features"])
    for key in FEATURE_COLUMNS:
        if key not in features:
            features[key] = 0.0
    return features


def _infer_weak_subjects(features: dict) -> list:
    """Derive weak subjects from per-subject features if present."""
    weak = []
    for key in sorted(features.keys()):
        if key.startswith("subject_") and key != "subject_accuracy":
            acc = float(features[key] or 0)
            if acc < 70:
                weak.append({
                    "subject_code": key.replace("subject_", "").upper(),
                    "accuracy": round(acc, 2),
                })
    return weak


def _infer_knowledge_gaps(features: dict) -> list:
    gaps = []
    if "accuracy" in features:
        acc = float(features.get("accuracy", 0) or 0)
        if acc < 70:
            gaps.append({
                "topic_name": "Core concepts",
                "mastery_level": round(acc, 2),
                "gap_level": "HIGH" if acc < 40 else "MEDIUM",
                "recommended_hours": max(1, int((100 - acc) // 20)),
            })
    return gaps


def register_routes(app, model_manager, preprocessor, explainer,
                    recommender, planner, chatbot):
    """Register all API routes on the Flask app."""
    api = Blueprint("api", __name__, url_prefix="/api")

    # ---------------------------------------------------------------- #
    @api.get("/health")
    def health():
        return jsonify({"status": "ok", "models": list(model_manager.models.keys()),
                        "metrics": model_manager.metrics})

    # ---------------------------------------------------------------- #
    @api.post("/predict")
    def predict():
        payload = request.get_json(silent=True) or {}
        features = _extract_features(payload)
        model_name = payload.get("model_name", "ensemble")
        result = model_manager.predict(features, model_name=model_name)
        result["available"] = True
        result["input_features"] = features
        result["weak_subjects"] = _infer_weak_subjects(features)
        result["knowledge_gaps"] = _infer_knowledge_gaps(features)
        return jsonify(result)

    # ---------------------------------------------------------------- #
    @api.post("/explain/shap")
    def explain_shap():
        payload = request.get_json(silent=True) or {}
        features = _extract_features(payload)
        return jsonify({**explainer.shap(features),
                        "available": True, "input_features": features})

    # ---------------------------------------------------------------- #
    @api.post("/explain/lime")
    def explain_lime():
        payload = request.get_json(silent=True) or {}
        features = _extract_features(payload)
        return jsonify({**explainer.lime(features),
                        "available": True, "input_features": features})

    # ---------------------------------------------------------------- #
    @api.post("/recommend")
    def recommend():
        payload = request.get_json(silent=True) or {}
        context = payload.get("context", payload)
        if "weak_subjects" not in context:
            context["weak_subjects"] = _infer_weak_subjects(context)
        if "knowledge_gaps" not in context:
            context["knowledge_gaps"] = _infer_knowledge_gaps(context)
        if "predicted_grade" not in context and "predicted_score" in context:
            context["predicted_grade"] = map_score_to_grade(float(context["predicted_score"]))
        result = recommender.generate(context)
        result["available"] = True
        return jsonify(result)

    # ---------------------------------------------------------------- #
    @api.post("/planner")
    def plan():
        payload = request.get_json(silent=True) or {}
        context = payload.get("context", payload)
        if "knowledge_gaps" not in context:
            context["knowledge_gaps"] = _infer_knowledge_gaps(context)
        plan_type = payload.get("plan_type", payload.get("type", "WEEKLY"))
        total_hours = int(payload.get("total_hours", 20))
        result = planner.generate(context, plan_type=plan_type, total_hours=total_hours)
        result["available"] = True
        return jsonify(result)

    # ---------------------------------------------------------------- #
    @api.post("/chat")
    def chat():
        payload = request.get_json(silent=True) or {}
        message = payload.get("message", "")
        provider = payload.get("provider")
        context = payload.get("context")
        history = payload.get("history")
        result = chatbot.chat(message, provider=provider, context=context, history=history)
        result["available"] = True
        return jsonify(result)

    # ---------------------------------------------------------------- #
    @api.post("/train")
    def train():
        """Endpoint to (re)train models with a provided dataset."""
        payload = request.get_json(silent=True) or {}
        if payload.get("data") is None:
            df = model_manager.generate_synthetic_dataset()
        else:
            df = pd.DataFrame(payload["data"])
        model_manager.train(df, preprocessor)
        model_manager.save()
        return jsonify({"available": True, "metrics": model_manager.metrics})

    # ---------------------------------------------------------------- #
    @api.get("/models")
    def models():
        return jsonify({
            "available": True,
            "models": list(model_manager.models.keys()),
            "metrics": model_manager.metrics,
            "features": FEATURE_COLUMNS,
        })

    app.register_blueprint(api)
