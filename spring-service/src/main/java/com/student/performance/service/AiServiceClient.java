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

    /**
     * Generates fresh MCQs for a topic the student just studied. The
     * {@code avoid} list holds questions the student already has, so the model
     * does not repeat them.
     *
     * @param concept what the test is actually about - the topic description or
     *                the heading of the video. Naming it keeps the questions on
     *                that concept instead of drifting across the whole subject.
     * @param sourceContent what the resource actually said up to the point the
     *                student reached - video captions or page text. This is what
     *                the questions are written from, so they are about the lesson
     *                rather than about the topic name. Empty falls back to
     *                asking about the concept alone.
     */
    Map<String, Object> generateQuestions(String topic, String subject, String resourceTitle,
                                          String difficulty, int count, java.util.List<String> avoid,
                                          String concept, String sourceContent);

    /**
     * Reads the text of a learning resource so questions can be written from it.
     *
     * <p>For a video {@code maxSeconds} trims the captions to the position the
     * student had reached, which is how "I have learned up to here" is honoured.
     * A page has no position and is returned whole.
     *
     * @return a map carrying {@code available}, {@code text}, {@code source}
     *         (TRANSCRIPT or PAGE), {@code durationSeconds} and a human
     *         readable {@code message} when nothing could be read. Never null.
     */
    Map<String, Object> extractContent(String url, String kind, Integer maxSeconds);
}
