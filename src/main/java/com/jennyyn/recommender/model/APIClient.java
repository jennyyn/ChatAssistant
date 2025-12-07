package com.jennyyn.recommender.model;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.Properties;

public class APIClient {
    private static APIClient instance;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    // Private constructor (Singleton)
    private APIClient() {
        Properties props = loadProperties();

        this.apiKey = props.getProperty("OPENAI_API_KEY");
        this.model = props.getProperty("MODEL", "gpt-4o-mini");

        this.httpClient = HttpClient.newHttpClient();

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: Missing API Key in config.properties");
        }
    }

    // access to the single instance
    public static APIClient getInstance() {
        if (instance == null) {
            instance = new APIClient();
        }
        return instance;
    }

    // Load the config.properties file from resources/
    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                System.err.println("WARNING: config.properties not found in resources/");
                return props;
            }
            props.load(input);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return props;
    }

    // Getters
    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }
}
