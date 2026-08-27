"""Entry point for the AI service. Run: python run.py"""
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent / ".env")

from app import app

if __name__ == "__main__":
    port = int(os.environ.get("FLASK_PORT", 5000))
    app.run(host=os.environ.get("FLASK_HOST", "0.0.0.0"), port=port,
            debug=os.environ.get("FLASK_DEBUG", "1") == "1")
