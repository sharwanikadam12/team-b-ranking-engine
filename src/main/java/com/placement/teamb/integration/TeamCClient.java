package com.placement.teamb.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Low-level HTTP client for Team C's REST API.
 *
 * Base URL is read from the {@code teamc.base-url} property so that
 * local dev (or Render) can point to the correct instance without
 * changing source code.
 *
 * All credentials/API keys are intentionally absent — Team C's current
 * API is open. If authentication is added in future, it should be
 * injected as environment variables, never hard-coded.
 */
@Service
public class TeamCClient {

    private static final Logger log =
            LoggerFactory.getLogger(TeamCClient.class);

    private final String baseUrl;
    private final long timeoutSeconds;
    private final HttpClient httpClient;

    public TeamCClient(
            @Value("${teamc.base-url}") String baseUrl,
            @Value("${teamc.timeout-seconds:30}") long timeoutSeconds) {

        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        log.info("TeamCClient initialised — base URL: {}", baseUrl);
    }

    /**
     * GET /api/v1/students
     * Returns raw JSON string; parsing is done in the calling service
     * so we keep this class responsible only for transport.
     */
    public String getStudents() {
        return sendGetRequest(baseUrl + "/api/v1/students");
    }

    /**
     * GET /api/v1/drives
     */
    public String getDrives() {
        return sendGetRequest(baseUrl + "/api/v1/drives");
    }

    /**
     * GET /api/v1/applications
     */
    public String getApplications() {
        return sendGetRequest(baseUrl + "/api/v1/applications");
    }

    /**
     * POST a JSON body to a URL.
     * Used for ranking write-back when Team C implements the endpoint.
     *
     * @param url  full URL to POST to
     * @param body JSON string body
     * @return response body as a String
     * @throws TeamCException if the request fails or Team C returns non-2xx
     */
    public String postJson(String url, String body) {

        try {
            log.info("POST {} — body length {}", url, body.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            log.info("POST {} -> HTTP {}", url, response.statusCode());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TeamCException(
                        "Team C returned HTTP " + response.statusCode()
                        + " on POST " + url
                        + ": " + response.body());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TeamCException("Unable to POST to Team C: " + url, e);
        }
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private String sendGetRequest(String url) {

        try {
            log.info("GET {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            log.info("GET {} -> HTTP {}", url, response.statusCode());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TeamCException(
                        "Team C returned HTTP " + response.statusCode()
                        + " for GET " + url
                        + ": " + response.body());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TeamCException(
                    "Unable to connect to Team C at " + url, e);
        }
    }
}