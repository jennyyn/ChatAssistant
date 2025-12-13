package com.jennyyn.recommender.service;

import com.jennyyn.recommender.model.APIClient;
import com.jennyyn.recommender.model.RewriteResult;
import com.jennyyn.recommender.model.WritingStrategy;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class APIService {

    private final APIClient apiClient;
    private final HttpClient httpClient;
    public APIService() {
        this.apiClient = APIClient.getInstance();
        this.httpClient = apiClient.getHttpClient();
    }

    /*Sends text to OpenAI and returns the rewritten text*/
    public RewriteResult rewriteText(String originalText, WritingStrategy strategy) throws Exception {
        String prompt = strategy.buildPrompt(originalText);

        // Build JSON body
        JsonObject json = new JsonObject();
        json.addProperty("model", apiClient.getModel());

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        json.add("messages", messages);

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiClient.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();

        // Send request (throws IOException, InterruptedException)
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse response
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
            JsonObject firstChoice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (firstChoice.has("message") && firstChoice.getAsJsonObject("message").has("content")) {
                String rewrittenText = firstChoice.getAsJsonObject("message").get("content").getAsString();
                return new RewriteResult(rewrittenText);
            } else {
                throw new Exception("Unexpected API response: 'message' or 'content' missing");
            }
        } else if (responseJson.has("error")) {
            String errorMessage = responseJson.getAsJsonObject("error").get("message").getAsString();
            throw new Exception("Failed to get response from API");
        } else {
            throw new Exception("Unexpected API response format");
        }
    }


}
