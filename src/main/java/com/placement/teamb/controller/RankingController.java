package com.placement.teamb.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.placement.teamb.integration.RankingWriteBackService;
import com.placement.teamb.integration.TeamCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.integration.TeamBCandidateService;
import com.placement.teamb.model.RankingCandidate;
import com.placement.teamb.ranking.RankingDispatcher;

/**
 * Team B REST API.
 *
 * Endpoints:
 *   GET  /health                             — liveness / readiness check
 *   GET  /api/v1/rankings/eligible/{driveId} — list eligible candidates from Team C
 *   POST /api/v1/rankings/compute            — rank a manually-supplied candidate pool
 *   POST /api/v1/rankings/rank/{driveId}     — full pipeline: fetch + rank + optional write-back
 */
@RestController
@CrossOrigin(origins = "*")
public class RankingController {

    private static final Logger log =
            LoggerFactory.getLogger(RankingController.class);

    private final RankingDispatcher rankingDispatcher;
    private final TeamBCandidateService candidateService;
    private final RankingWriteBackService writeBackService;

    public RankingController(
            RankingDispatcher rankingDispatcher,
            TeamBCandidateService candidateService,
            RankingWriteBackService writeBackService) {

        this.rankingDispatcher = rankingDispatcher;
        this.candidateService = candidateService;
        this.writeBackService = writeBackService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simple health check required for Render deployment verification.
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("service", "team-b-ranking-engine");
        body.put("version", "1.0.0");
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Eligible candidates (read from Team C)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches eligible candidates for a drive from Team C without ranking them.
     * Useful for debugging and inspecting what Team C has.
     *
     * GET /api/v1/rankings/eligible/{driveId}
     */
    @GetMapping("/api/v1/rankings/eligible/{driveId}")
    public ResponseEntity<Map<String, Object>> getEligibleCandidates(
            @PathVariable String driveId) {

        log.info("GET /api/v1/rankings/eligible/{}", driveId);

        try {
            List<RankingCandidate> candidates =
                    candidateService.getEligibleCandidates(driveId);

            Map<String, Object> response = new HashMap<>();
            response.put("driveId", driveId);
            response.put("candidateCount", candidates.size());
            response.put("candidates", candidates);

            return ResponseEntity.ok(response);

        } catch (TeamCException e) {
            log.error("Team C error for drive {}: {}", driveId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(errorBody("Team C unavailable: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compute (manual candidate pool supplied by caller)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ranks a manually-supplied candidate pool without fetching from Team C.
     * Useful for testing the ranking algorithms in isolation.
     *
     * POST /api/v1/rankings/compute
     * Body: { "driveId":"DRV001", "candidatePool":[...], "algorithm":"MERGE_SORT" }
     */
    @PostMapping("/api/v1/rankings/compute")
    public ResponseEntity<RankingResult> computeRanking(
            @RequestBody RankingRequest request) {

        log.info("POST /api/v1/rankings/compute — drive={} algorithm={}",
                request != null ? request.getDriveId() : "null",
                request != null ? request.getAlgorithm() : "null");

        RankingResult result = rankingDispatcher.computeRanking(request);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full pipeline: fetch from Team C + rank + optional write-back
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full pipeline:
     * 1. Fetch eligible candidates for driveId from Team C.
     * 2. Rank them using the specified algorithm (default: MERGE_SORT).
     * 3. Optionally send results back to Team C (controlled by feature flag).
     *
     * POST /api/v1/rankings/rank/{driveId}
     * Body (optional): { "algorithm":"HEAP_TOPK", "constraints":{"topK":5} }
     */
    @PostMapping("/api/v1/rankings/rank/{driveId}")
    public ResponseEntity<Map<String, Object>> rankForDrive(
            @PathVariable String driveId,
            @RequestBody(required = false) RankingRequest options) {

        log.info("POST /api/v1/rankings/rank/{}", driveId);

        try {
            // Step 1: Fetch eligible candidates from Team C
            List<RankingCandidate> candidates =
                    candidateService.getEligibleCandidates(driveId);

            if (candidates.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("driveId", driveId);
                response.put("status", "NO_ELIGIBLE_CANDIDATES");
                response.put("message",
                        "No eligible candidates found for drive " + driveId
                        + ". Check Team C /api/v1/applications.");
                return ResponseEntity.ok(response);
            }

            // Step 2: Build ranking request
            String algorithm = (options != null && options.getAlgorithm() != null)
                    ? options.getAlgorithm()
                    : "MERGE_SORT";

            Map<String, Object> constraints = new HashMap<>();
            if (options != null && options.getConstraints() != null) {
                constraints.putAll(options.getConstraints());
            }
            if (!constraints.containsKey("topK")) {
                constraints.put("topK", candidates.size());
            }

            RankingRequest request = new RankingRequest(
                    driveId,
                    candidates,
                    "TEAM-CURRENT",
                    algorithm,
                    "SCORE_DESCENDING",
                    constraints
            );

            // Step 3: Rank
            RankingResult result = rankingDispatcher.computeRanking(request);

            // Step 4: Write-back (only if enabled via feature flag)
            boolean written = writeBackService.sendRanking(driveId, result);

            // Step 5: Build response
            Map<String, Object> response = new HashMap<>();
            response.put("driveId", driveId);
            response.put("ranking", result);
            response.put("writeBackSent", written);
            response.put("writeBackNote", written
                    ? "Ranking sent to Team C successfully."
                    : "Write-back disabled — Team C does not yet implement "
                    + "POST /api/v1/drive-rankings. "
                    + "Set TEAM_B_RANKING_WRITE_ENABLED=true to enable.");

            return ResponseEntity.ok(response);

        } catch (TeamCException e) {
            log.error("Team C error for drive {}: {}", driveId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(errorBody("Team C unavailable: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exception handler
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(TeamCException.class)
    public ResponseEntity<Map<String, Object>> handleTeamCException(
            TeamCException e) {

        log.error("Unhandled TeamCException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody("Team C error: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception e) {

        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Internal error: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", Instant.now().toString());
        return error;
    }
}