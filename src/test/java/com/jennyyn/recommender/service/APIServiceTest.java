package com.jennyyn.recommender.service;

import com.jennyyn.recommender.model.APIClient;
import com.jennyyn.recommender.model.RateLimitException;
import com.jennyyn.recommender.model.RewriteResult;
import com.jennyyn.recommender.model.WritingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class APIServiceTest {

    @Mock APIClient mockApiClient;
    @Mock HttpClient mockHttpClient;
    @Mock WritingStrategy mockStrategy;
    @Mock HttpResponse<String> mockResponse;

    APIService service;

    @BeforeEach
    void setup() {
        service = new APIService(mockApiClient, mockHttpClient);
        when(mockApiClient.getApiKey()).thenReturn("abc123");
        when(mockApiClient.getModel()).thenReturn("gpt-4o-mini");
    }

    @Test
    void testRewriteTextSuccess() throws Exception {
        when(mockStrategy.buildPrompt("Hello")).thenReturn("Prompted Text");

        String json = """
                {
                  "choices": [
                    { "message": { "content": "Rewritten OK" } }
                  ]
                }
                """;
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

        RewriteResult result = service.rewriteText("Hello", mockStrategy);

        assertEquals("Rewritten OK", result.getRewrittenText());
    }

    @Test
    void testRateLimitException() throws Exception {
        when(mockStrategy.buildPrompt(anyString())).thenReturn("Prompt");

        String json = """
                {
                  "error": { "message": "Rate limit exceeded" }
                }
                """;
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

        RateLimitException ex = assertThrows(RateLimitException.class,
                () -> service.rewriteText("Hi", mockStrategy));

        assertTrue(ex.getMessage().contains("Rate limit"));
    }

    @Test
    void testGenericAPIError() throws Exception {
        when(mockStrategy.buildPrompt(anyString())).thenReturn("Prompt");

        String json = """
                {
                  "error": { "message": "Bad API request" }
                }
                """;
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

        Exception ex = assertThrows(Exception.class,
                () -> service.rewriteText("Hi", mockStrategy));

        assertTrue(ex.getMessage().contains("Bad API request"));
    }

    @Test
    void testTimeoutException() throws Exception {
        when(mockStrategy.buildPrompt(anyString())).thenReturn("Prompt");

        when(mockHttpClient.send(any(), any()))
                .thenThrow(new java.net.http.HttpTimeoutException("timeout"));

        Exception ex = assertThrows(Exception.class,
                () -> service.rewriteText("Hi", mockStrategy));

        assertTrue(ex.getMessage().contains("timed out"));
    }

    @Test
    void testRewriteTextAsyncCancel() throws InterruptedException {
        when(mockStrategy.buildPrompt(anyString())).thenReturn("Prompt");

        // Simulate a long-running request
        doAnswer(invocation -> {
            Thread.sleep(2000);
            return mockResponse;
        }).when(mockHttpClient).send(any(), any());

        boolean[] errorCalled = {false};
        boolean[] successCalled = {false};

        service.rewriteTextAsync(
                "Hi",
                mockStrategy,
                r -> successCalled[0] = true,
                e -> errorCalled[0] = e instanceof InterruptedException,
                () -> {}
        );

        Thread.sleep(100); // let the async thread start
        service.cancel();

        Thread.sleep(100); // give it a moment to handle cancel

        assertFalse(successCalled[0]);
        assertTrue(errorCalled[0]);
    }
}
