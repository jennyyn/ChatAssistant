package com.jennyyn.recommender.model;

public class ProfessionalStrategy implements WritingStrategy {

    @Override
    public String buildPrompt(String originalText) {
        return "Rewrite the following text in a clear, professional tone suitable for work or business:\n\n" + originalText;
    }
}

