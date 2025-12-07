package com.jennyyn.recommender.model;

public class CreativeStrategy implements WritingStrategy {

    @Override
    public String buildPrompt(String originalText) {
        return "Rewrite the following text in a creative, expressive style:\n\n" + originalText;
    }
}
