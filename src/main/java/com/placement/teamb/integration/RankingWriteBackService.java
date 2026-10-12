package com.placement.teamb.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends Team B's computed ranking result back to Team C for storage.
 *
 * ─── CURRENT STATUS (2026-10-11) ─────────────────────────────────────────────
 * Team C does NOT yet expose a ranking write endpoint.
 * GET  /api/v1/rankings  → 404
 * POST /api/v1/drive-rankings → (proposed — not implemented)
 *
 * This service is therefore DISABLED by default via:
 *   ranking.writeback.enabled=false  (set in application.properties)
 *
 * When Team C implements the endpoint, set:
 *   TEAM_B_RANKING_WRITE_ENABLED=true
 *   TEAM_B_RANKING_WRITE_URL=https://rit-placement-team-c-backend.onrender.com/api/v1/drive-rankings
 *
 * ─── PROPOSED Team C Endpoint Contract ───────────────────────────────────────
 *
 * POST /api/v1/drive-rankings
 * Request:
 * {
 *   "driveId": "DRV001",
 *   "rankingId": "RANKING_READY",
 *   "algorithm": "MERGE_SORT",
 *   "graphVersion": "TEAM-CURRENT",
 *   "tieBreakRule": "Higher score first, student ID as tie-break",
 *   "rankedCandidates": [
 *     { "rank": 1, "studentId": "STU001", "totalScore": 11.28 },
 *     { "rank": 2, "studentId": "STU006", "totalScore": 11.05 }
 *   ]
 * }
 *
 * Response (200):
 * {
 *   "success": true,
 *   "driveId": "DRV001",
 *   "savedAt": "2026-10-11T22:00:00Z"
 * }
 *
 * Idempotency: Team B sends the drive ID as the natural idempotency key.
 * Team C should upsert (findOneAndReplace with { driveId } filter) to avoid
 * duplicate records on repeated calls.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
public class RankingWriteBackService {

    private static final Logger log =
            LoggerFactory.getLogger(RankingWriteBackService.class);

    private final TeamCClient teamCClient;
    private final ObjectMapper objectMapper;
    private final boolean writebackEnabled;
    private final String writebackUrl;

    public RankingWriteBackService(
            TeamCClient teamCClient,
            @Value("${ranking.writeback.enabled:false}") boolean writebackEnabled,
            @Value("${ranking.writeback.url:}") String writebackUrl) {

        this.teamCClient = teamCClient;
        this.objectMapper = new ObjectMapper();
        this.writebackEnabled = writebackEnabled;
        this.writebackUrl = writebackUrl;

        if (writebackEnabled) {
            log.info("Ranking write-back ENABLED → {}", writebackUrl);
        } else {
            log.info("Ranking write-back DISABLED "
                    + "(set TEAM_B_RANKING_WRITE_ENABLED=true to enable "
                    + "once Team C implements POST /api/v1/drive-rankings)");
        }
    }

    /**
     * Sends the ranking result to Team C.
     *
     * @param driveId the drive these rankings belong to
     * @param result  the computed ranking result
     * @return true if write-back was attempted AND succeeded;
     *         false if disabled or if Team C is unavailable
     */
    public boolean sendRanking(String driveId, RankingResult result) {

        if (!writebackEnabled) {
            log.info("Write-back skipped (disabled) for drive {}", driveId);
            return false;
        }

        try {
            String body = buildRequestBody(driveId, result);
            log.info("Sending ranking for drive {} to {}", driveId, writebackUrl);
            String response = teamCClient.postJson(writebackUrl, body);
            log.info("Write-back succeeded for drive {}: {}", driveId, response);
            return true;
        } catch (Exception e) {
            log.error("Write-back failed for drive {}: {}", driveId, e.getMessage());
            return false;
        }
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private String buildRequestBody(String driveId, RankingResult result)
            throws Exception {

        List<Map<String, Object>> candidates = new ArrayList<>();

        if (result.getOrderedCandidates() != null) {
            for (CandidateScore cs : result.getOrderedCandidates()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("rank", cs.getRank());
                entry.put("studentId", cs.getStudentId());
                entry.put("totalScore", cs.getTotalScore());
                candidates.add(entry);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("driveId", driveId);
        body.put("rankingId", result.getRankingId());
        body.put("algorithm", result.getAlgorithm());
        body.put("graphVersion", result.getGraphVersion());
        body.put("tieBreakRule", result.getTieBreakRule());
        body.put("rankedCandidates", candidates);

        return objectMapper.writeValueAsString(body);
    }
}
