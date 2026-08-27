"""
AI-Powered Student Performance Prediction & Personalized Learning System
Python AI Service - Main Flask application.

Modules:
  - Data preprocessing (missing values, scaling, encoding, transformation)
  - ML model training (Random Forest, Gradient Boosting, XGBoost, Decision Tree)
  - Prediction (grade, risk, score, confidence)
  - Explainable AI (SHAP, LIME, feature importance)
  - Weak subject detection, knowledge gap detection
  - Recommendation engine, study planner
  - AI Chatbot (Gemini / OpenAI / rule-based fallback)
"""

import os
import logging

from flask import Flask
from flask_cors import CORS

from app.preprocessing import DataPreprocessor
from app.models import ModelManager
from app.recommendation import RecommendationEngine, StudyPlanner
from app.chatbot import ChatbotService
from app.explainability import Explainer

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger("ai-service")


def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app, resources={r"/api/*": {"origins": "*"}})

    model_dir = os.environ.get("MODEL_DIR", os.path.join(os.path.dirname(__file__), "..", "models"))
    model_manager = ModelManager(model_dir=model_dir)
    preprocessor = DataPreprocessor()
    explainer = Explainer(model_manager)
    recommender = RecommendationEngine()
    planner = StudyPlanner()
    chatbot = ChatbotService()

    # Ensure models are ready on startup (train once from synthetic dataset)
    model_manager.ensure_models(preprocessor)

    # Register blueprints / routes
    from app.routes import register_routes
    register_routes(app, model_manager, preprocessor, explainer,
                    recommender, planner, chatbot)

    @app.get("/")
    def index():
        return {"service": "student-performance-ai", "status": "running"}

    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.environ.get("FLASK_PORT", 5000))
    app.run(host=os.environ.get("FLASK_HOST", "0.0.0.0"), port=port,
            debug=os.environ.get("FLASK_DEBUG", "1") == "1")
