package com.placement.teamb.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Low-level HTTP client for Team C's REST API.
 *
 * Base URL is read from the {@code teamc.base-url} property so that
 * local dev (or Render) can point to the correct instance without
 * changing source code.
 *
 * Authentication: GET endpoints (students, drives, applications) are
 * unauthenticated. POST endpoints (ranking write-back) require a Bearer token
 * supplied via the {@code teamc.write-token} property / {@code TEAM_C_WRITE_TOKEN}
 * environment variable. The token is never logged.
 */
@Service
public class TeamCClient {

    private static final Logger log =
            LoggerFactory.getLogger(TeamCClient.class);

    private final String baseUrl;
    private final long timeoutSeconds;
    /** Bearer token for authenticated POST endpoints. Empty string = no header sent. */
    private final String writeToken;
    private final HttpClient httpClient;

    @Autowired
    public TeamCClient(
            @Value("${teamc.base-url}") String baseUrl,
            @Value("${teamc.timeout-seconds:30}") long timeoutSeconds,
            @Value("${teamc.write-token:}") String writeToken) {

        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.writeToken = writeToken != null ? writeToken : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        log.info("TeamCClient initialised — base URL: {}", baseUrl);
        if (!this.writeToken.isBlank()) {
            log.info("TeamCClient: write-token configured (value not logged)");
        } else {
            log.info("TeamCClient: no write-token configured "
                    + "(set TEAM_C_WRITE_TOKEN env var to enable authenticated POSTs)");
        }
    }

    /**
     * Backward-compatible constructor without write token (omits Bearer header).
     */
    public TeamCClient(String baseUrl, long timeoutSeconds) {
        this(baseUrl, timeoutSeconds, "");
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
     * POST a JSON body to a URL with an optional Bearer token.
     * Used for ranking write-back to Team C's internal endpoint.
     *
     * If {@code teamc.write-token} is configured (non-blank), the header
     * {@code Authorization: Bearer <token>} is added automatically.
     * If the token is blank, the header is omitted entirely (Team C will
     * respond with 401, which is surfaced as a {@link TeamCException}).
     *
     * The token value is NEVER written to any log output.
     *
     * @param url  full URL to POST to
     * @param body JSON string body
     * @return response body as a String
     * @throws TeamCException if the request fails or Team C returns non-2xx
     */
    public String postJson(String url, String body) {

        try {
            log.info("POST {} — body length {}", url, body.length());

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            // Add Authorization header only when a token is configured.
            // An empty/blank token means Team C auth is not yet set up locally.
            if (!writeToken.isBlank()) {
                builder.header("Authorization", "Bearer " + writeToken);
            }

            HttpRequest request = builder.build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            log.info("POST {} -> HTTP {}", url, response.statusCode());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 401) {
                    log.error("Team C write-back returned 401 Unauthorized — "
                            + "verify TEAM_C_WRITE_TOKEN in environment");
                    throw new TeamCException(
                            "Team C rejected ranking write: 401 Unauthorized "
                            + "(check TEAM_C_WRITE_TOKEN)");
                }
                throw new TeamCException(
                        "Team C returned HTTP " + response.statusCode()
                        + " on POST " + url
                        + ": " + response.body());
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TeamCException(
                    "Unable to connect to Team C at " + url, e);
        }
    }
}