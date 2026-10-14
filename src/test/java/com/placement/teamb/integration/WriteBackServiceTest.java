package com.placement.teamb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.teamb.dto.EligibleCandidate;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and integration tests for {@link RankingWriteBackService} and {@link TeamCClient}.
 *
 * Uses a lightweight, local JDK {@link HttpServer} rather than bytecode-manipulating mocks.
 * This guarantees compatibility with modern JDKs (including Java 25), avoids external network calls,
 * and tests the real HTTP protocol interaction (headers, Bearer token, JSON payloads, status codes).
 */
class WriteBackServiceTest {

    private HttpServer server;
    private String serverBaseUrl;
    private String writeBackEndpoint;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final List<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "{\"success\":true,\"savedAt\":\"2026-10-14T12:00:00Z\"}";

    private final ObjectMapper mapper = new ObjectMapper();

    static class RecordedRequest {
        String method;
        String path;
        Headers headers;
        String body;
    }

    @BeforeEach
    void setUp() throws IOException {
        recordedRequests.clear();
        requestCount.set(0);
        responseStatus = 200;
        responseBody = "{\"success\":true,\"savedAt\":\"2026-10-14T12:00:00Z\"}";

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            RecordedRequest req = new RecordedRequest();
            req.method = exchange.getRequestMethod();
            req.path = exchange.getRequestURI().getPath();
            req.headers = exchange.getRequestHeaders();
            req.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recordedRequests.add(req);
            requestCount.incrementAndGet();

            byte[] respBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        serverBaseUrl = "http://localhost:" + port;
        writeBackEndpoint = serverBaseUrl + "/internal/v1/drive-rankings";
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Builds a minimal RankingResult containing the given CandidateScore entries. */
    private RankingResult buildResult(List<CandidateScore> scores) {
        return new RankingResult(
                "RANKING_READY",
                scores,
                scores.stream().mapToDouble(CandidateScore::getTotalScore).sum(),
                "Higher score first, student ID as tie-break",
                "MERGE_SORT",
                "TEAM-CURRENT",
                scores.size()
        );
    }

    // ─── 1. Disabled guard ───────────────────────────────────────────────────

    @Test
    void writeBack_disabled_returnsFalseAndNeverCallsTeamC() {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "test-write-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client,
                false,          // writebackEnabled = false
                writeBackEndpoint
        );

        RankingResult result = buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        ));

        boolean sent = service.sendRanking("DRV001", result);

        assertThat(sent).isFalse();
        assertThat(requestCount.get()).isEqualTo(0);
    }

    // ─── 2. Enabled path calls endpoint with Bearer authentication ───────────

    @Test
    void writeBack_enabled_callsPostJsonWithCorrectUrlAndBearerAuth() {
        String token = "team-c-secret-token-xyz";
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, token);
        RankingWriteBackService service = new RankingWriteBackService(
                client,
                true,           // writebackEnabled = true
                writeBackEndpoint
        );

        RankingResult result = buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        ));

        boolean sent = service.sendRanking("DRV001", result);

        assertThat(sent).isTrue();
        assertThat(recordedRequests).hasSize(1);

        RecordedRequest req = recordedRequests.get(0);
        assertThat(req.method).isEqualTo("POST");
        assertThat(req.path).isEqualTo("/internal/v1/drive-rankings");
        assertThat(req.headers.getFirst("Authorization")).isEqualTo("Bearer " + token);
        assertThat(req.headers.getFirst("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void writeBack_enabled_noToken_omitsAuthorizationHeader() {
        // When writeToken is blank, Authorization header should not be sent
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "");
        RankingWriteBackService service = new RankingWriteBackService(
                client,
                true,
                writeBackEndpoint
        );

        RankingResult result = buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        ));

        boolean sent = service.sendRanking("DRV001", result);

        assertThat(sent).isTrue();
        assertThat(recordedRequests).hasSize(1);
        assertThat(recordedRequests.get(0).headers.getFirst("Authorization")).isNull();
    }

    // ─── 3. Payload structure and applicationId validation ───────────────────

    @Test
    void writeBack_enabled_payloadContainsAllRequiredFields() throws Exception {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        List<CandidateScore> scores = List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1),
                new CandidateScore("STU012", "APP008", 13.12, 2)
        );
        boolean sent = service.sendRanking("DRV001", buildResult(scores));

        assertThat(sent).isTrue();
        assertThat(recordedRequests).hasSize(1);

        JsonNode body = mapper.readTree(recordedRequests.get(0).body);

        // Required top-level fields
        assertThat(body.has("driveId")).isTrue();
        assertThat(body.get("driveId").asText()).isEqualTo("DRV001");

        assertThat(body.has("rankingId")).isTrue();
        assertThat(body.get("rankingId").asText()).isNotEmpty();
        assertThat(body.get("rankingId").asText()).startsWith("RNK-DRV001-MERGE_SORT-");

        assertThat(body.has("algorithm")).isTrue();
        assertThat(body.get("algorithm").asText()).isEqualTo("MERGE_SORT");

        assertThat(body.has("graphVersion")).isTrue();
        assertThat(body.get("graphVersion").asText()).isEqualTo("TEAM-CURRENT");

        assertThat(body.has("tieBreakRule")).isTrue();
        assertThat(body.get("tieBreakRule").asText()).isEqualTo("Higher score first, student ID as tie-break");

        // Ranked candidates array
        JsonNode rankedCandidates = body.get("rankedCandidates");
        assertThat(rankedCandidates).isNotNull();
        assertThat(rankedCandidates.isArray()).isTrue();
        assertThat(rankedCandidates.size()).isEqualTo(2);

        JsonNode first = rankedCandidates.get(0);
        assertThat(first.get("studentId").asText()).isEqualTo("STU001");
        assertThat(first.get("applicationId").asText()).isEqualTo("APP001");
        assertThat(first.get("rank").asInt()).isEqualTo(1);
        assertThat(first.get("totalScore").asDouble()).isEqualTo(14.28);

        JsonNode second = rankedCandidates.get(1);
        assertThat(second.get("studentId").asText()).isEqualTo("STU012");
        assertThat(second.get("applicationId").asText()).isEqualTo("APP008");
        assertThat(second.get("rank").asInt()).isEqualTo(2);
        assertThat(second.get("totalScore").asDouble()).isEqualTo(13.12);
    }

    @Test
    void writeBack_enabled_nullApplicationId_serialisedAsJsonNull() throws Exception {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        // Use the 3-arg (legacy) constructor — applicationId is null
        service.sendRanking("DRV001", buildResult(List.of(
                new CandidateScore("STU001", 14.28, 1)
        )));

        assertThat(recordedRequests).hasSize(1);
        JsonNode body = mapper.readTree(recordedRequests.get(0).body);
        JsonNode first = body.get("rankedCandidates").get(0);

        assertThat(first.has("applicationId")).isTrue();
        assertThat(first.get("applicationId").isNull()).isTrue();
    }

    // ─── 4. Unique and stable rankingId ──────────────────────────────────────

    @Test
    void writeBack_rankingIdIsStableForIdenticalPayload() throws Exception {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        List<CandidateScore> scores = List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        );

        service.sendRanking("DRV001", buildResult(scores));
        service.sendRanking("DRV001", buildResult(scores));

        assertThat(recordedRequests).hasSize(2);
        JsonNode body1 = mapper.readTree(recordedRequests.get(0).body);
        JsonNode body2 = mapper.readTree(recordedRequests.get(1).body);

        String id1 = body1.get("rankingId").asText();
        String id2 = body2.get("rankingId").asText();

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void writeBack_rankingIdIsDistinctForDifferentPayloads() throws Exception {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        service.sendRanking("DRV001", buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        )));
        service.sendRanking("DRV002", buildResult(List.of(
                new CandidateScore("STU002", "APP002", 12.00, 1)
        )));

        assertThat(recordedRequests).hasSize(2);
        JsonNode body1 = mapper.readTree(recordedRequests.get(0).body);
        JsonNode body2 = mapper.readTree(recordedRequests.get(1).body);

        String id1 = body1.get("rankingId").asText();
        String id2 = body2.get("rankingId").asText();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void writeBack_customRankingIdPreserved() throws Exception {
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        RankingResult customResult = new RankingResult(
                "CUSTOM-RUN-9999",
                List.of(new CandidateScore("STU001", "APP001", 14.28, 1)),
                14.28,
                "Higher score first, student ID as tie-break",
                "MERGE_SORT",
                "TEAM-CURRENT",
                1
        );

        service.sendRanking("DRV001", customResult);

        assertThat(recordedRequests).hasSize(1);
        JsonNode body = mapper.readTree(recordedRequests.get(0).body);
        assertThat(body.get("rankingId").asText()).isEqualTo("CUSTOM-RUN-9999");
    }

    // ─── 5. Error handling ───────────────────────────────────────────────────

    @Test
    void writeBack_serverReturns401_returnsFalseWithoutThrowing() {
        responseStatus = 401;
        responseBody = "{\"error\":\"Unauthorized\",\"message\":\"Invalid token\"}";

        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "bad-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        boolean sent = service.sendRanking("DRV001", buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        )));

        assertThat(sent).isFalse();
    }

    @Test
    void writeBack_serverReturns500_returnsFalseWithoutThrowing() {
        responseStatus = 500;
        responseBody = "{\"error\":\"Internal Server Error\"}";

        TeamCClient client = new TeamCClient(serverBaseUrl, 5L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, writeBackEndpoint);

        boolean sent = service.sendRanking("DRV001", buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        )));

        assertThat(sent).isFalse();
    }

    @Test
    void writeBack_serverUnreachable_returnsFalseWithoutThrowing() {
        // Point to an unused local port that will refuse connection immediately
        TeamCClient client = new TeamCClient("http://localhost:59999", 1L, "dummy-token");
        RankingWriteBackService service = new RankingWriteBackService(
                client, true, "http://localhost:59999/internal/v1/drive-rankings");

        boolean sent = service.sendRanking("DRV001", buildResult(List.of(
                new CandidateScore("STU001", "APP001", 14.28, 1)
        )));

        assertThat(sent).isFalse();
    }

    // ─── 6. GET endpoints do not require or use writeToken ───────────────────

    @Test
    void teamCClient_getRequests_succeedWithoutWriteToken() {
        responseStatus = 200;
        responseBody = "{\"data\":[]}";

        // Client without writeToken (legacy 2-arg constructor)
        TeamCClient client = new TeamCClient(serverBaseUrl, 5L);

        String students = client.getStudents();
        assertThat(students).isEqualTo("{\"data\":[]}");

        String drives = client.getDrives();
        assertThat(drives).isEqualTo("{\"data\":[]}");

        String applications = client.getApplications();
        assertThat(applications).isEqualTo("{\"data\":[]}");

        // Ensure GET requests carried NO Authorization header
        for (RecordedRequest r : recordedRequests) {
            assertThat(r.method).isEqualTo("GET");
            assertThat(r.headers.getFirst("Authorization")).isNull();
        }
    }

    // ─── 7. EligibleApplicationService propagation and filtering ─────────────

    @Test
    void eligibleApplicationService_filtersByDriveAndEligible_propagatesApplicationId() {
        responseStatus = 200;
        responseBody = "{"
                + "\"data\": ["
                + "  {\"_id\": \"APP001\", \"studentId\": \"STU001\", \"driveId\": \"DRV001\", \"eligibility\": {\"result\": \"ELIGIBLE\"}},"
                + "  {\"_id\": \"APP002\", \"studentId\": \"STU002\", \"driveId\": \"DRV001\", \"eligibility\": {\"result\": \"NOT_ELIGIBLE\"}},"
                + "  {\"_id\": \"APP003\", \"studentId\": \"STU003\", \"driveId\": \"DRV002\", \"eligibility\": {\"result\": \"ELIGIBLE\"}}"
                + "]}";

        TeamCClient client = new TeamCClient(serverBaseUrl, 5L);
        EligibleApplicationService service = new EligibleApplicationService(client);

        List<EligibleCandidate> candidates = service.getEligibleApplications("DRV001");
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getApplicationId()).isEqualTo("APP001");
        assertThat(candidates.get(0).getStudentId()).isEqualTo("STU001");
        assertThat(candidates.get(0).getDriveId()).isEqualTo("DRV001");

        List<String> studentIds = service.getEligibleStudentIds("DRV001");
        assertThat(studentIds).containsExactly("STU001");
    }
}
