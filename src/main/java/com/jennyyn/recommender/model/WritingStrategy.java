package com.jennyyn.recommender.model;

public interface WritingStrategy {
    //takes original text and returns the prompt to send to OpenAPI
    String buildPrompt(String originalText);
}
