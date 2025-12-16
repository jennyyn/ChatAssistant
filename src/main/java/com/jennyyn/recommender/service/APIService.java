package com.jennyyn.recommender.service;

import com.jennyyn.recommender.model.APIClient;
import com.jennyyn.recommender.model.RateLimitException;
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

    private volatile Thread currentRequestThread = null;
    volatile boolean cancelRequested = false;

    private final APIClient apiClient;
    private final HttpClient httpClient;

    private static final int REQUEST_TIMEOUT_SECONDS = 10; // Timeout for network issues

    public APIService() {
        this.apiClient = APIClient.getInstance();
        this.httpClient = apiClient.getHttpClient();
    }

    // TESTING CONSTRUCTOR — allows injecting mocks
    public APIService(APIClient apiClient, HttpClient httpClient) {
        this.apiClient = apiClient;
        this.httpClient = httpClient;
    }

    boolean isCancelRequested() {
        return cancelRequested;
    }

    public void rewriteTextAsync(
            String originalText,
            WritingStrategy strategy,
            java.util.function.Consumer<RewriteResult> onSuccess,
            java.util.function.Consumer<Exception> onError,
            Runnable onFinally
    ) {
        cancelRequested = false;

        Thread worker = new Thread(() -> {
            try {
                if (cancelRequested) return;

                RewriteResult result = rewriteText(originalText, strategy);

                if (!cancelRequested) onSuccess.accept(result);

            } catch (Exception e) {
                // Always notify the caller of the exception.
                // The UI can treat InterruptedException as "cancelled".
                onError.accept(e);
            } finally {
                onFinally.run();
            }
        });

        currentRequestThread = worker;
        worker.start();
    }

    public void cancel() {
        cancelRequested = true;
        if (currentRequestThread != null) {
            currentRequestThread.interrupt();
        }
    }

    /*Sends text to OpenAI and returns the rewritten text*/
    RewriteResult rewriteText(String originalText, WritingStrategy strategy) throws Exception {
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
                .timeout(java.time.Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS)) // <-- timeout added
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();


        if (cancelRequested) throw new InterruptedException("Request cancelled.");

        HttpResponse<String> response;

        try {
            // Use blocking send(), but now it will fail if timeout is reached
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new Exception("Request timed out. Check your internet connection.");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw new InterruptedException("Request cancelled.");
            }
            throw new Exception("Unable to contact API. Check your internet connection.");
        }

        if (cancelRequested) throw new InterruptedException("Request cancelled.");


        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

        // SUCCESS CASE
        if (responseJson.has("choices")) {
            JsonObject firstChoice = responseJson
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject();

            if (firstChoice.has("message") &&
                    firstChoice.getAsJsonObject("message").has("content")) {

                String rewrittenText = firstChoice
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                return new RewriteResult(rewrittenText);
            }

            throw new Exception("Invalid API response.");
        }

        // API ERROR CASE
        if (responseJson.has("error")) {
            JsonObject errorObj = responseJson.getAsJsonObject("error");
            String errorMsg = errorObj.has("message") ? errorObj.get("message").getAsString() : "Unknown API error";

            // Detect rate-limit from message
            if (errorMsg.toLowerCase().contains("rate limit")) {
                throw new RateLimitException("Rate limit exceeded. Please wait a few seconds.");
            } else {
                throw new Exception(errorMsg);
            }
        }

        // Unknown response shape
        throw new Exception("Unexpected API response format.");
    }


}
