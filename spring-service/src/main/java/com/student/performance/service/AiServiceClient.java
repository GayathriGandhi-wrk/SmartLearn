package com.student.performance.service;

import java.util.Map;

public interface AiServiceClient {

    /**
     * Calls the Python Flask AI service to predict student performance.
     * Returns raw JSON response as a Map.
     */
    Map<String, Object> predict(Map<String, Object> features);

    /**
     * Requests SHAP explanation for a prediction.
     */
    Map<String, Object> explainShap(Map<String, Object> features);

    /**
     * Requests LIME explanation for a prediction.
     */
    Map<String, Object> explainLime(Map<String, Object> features);

    /**
     * Requests recommendation list from the AI engine.
     */
    Map<String, Object> recommend(Map<String, Object> context);

    /**
     * Requests AI-generated study plan.
     */
    Map<String, Object> generatePlan(Map<String, Object> context);

    /**
     * Chatbot request routed to Gemini or OpenAI.
     */
    Map<String, Object> chat(String message, String provider, java.util.List<Map<String, String>> history);
}
