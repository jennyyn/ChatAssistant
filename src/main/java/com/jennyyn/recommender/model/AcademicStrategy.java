package com.jennyyn.recommender.model;


public class AcademicStrategy implements WritingStrategy {

    @Override
    public String buildPrompt(String originalText) {
        return "Rewrite the following text in a formal, academic style, using precise language and proper grammar:\n\n" + originalText;
    }
}
