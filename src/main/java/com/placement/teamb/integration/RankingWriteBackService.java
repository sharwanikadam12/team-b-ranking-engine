package com.placement.teamb.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends Team B's computed ranking result back to Team C for storage.
 *
 * ─── CURRENT STATUS ───────────────────────────────────────────────────────────
 * Write-back is DISABLED by default:
 *   ranking.writeback.enabled=false  (set in application.properties)
 *
 * To enable once Team C confirms the endpoint is live:
 *   TEAM_B_RANKING_WRITE_ENABLED=true
 *   TEAM_B_RANKING_WRITE_URL=https://rit-placement-team-c-backend.onrender.com/internal/v1/drive-rankings
 *   TEAM_C_WRITE_TOKEN=<bearer-token-from-team-c>
 *
 * ─── Team C Endpoint Contract ─────────────────────────────────────────────────
 *
 * POST /internal/v1/drive-rankings
 * Authorization: Bearer <token>
 * Request:
 * {
 *   "driveId": "DRV001",
 *   "rankingId": "RANKING_READY",
 *   "algorithm": "MERGE_SORT",
 *   "graphVersion": "TEAM-CURRENT",
 *   "tieBreakRule": "Higher score first, student ID as tie-break",
 *   "rankedCandidates": [
 *     { "rank": 1, "studentId": "STU001", "applicationId": "APP001", "totalScore": 14.28 },
 *     { "rank": 2, "studentId": "STU012", "applicationId": "APP008", "totalScore": 13.12 }
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
                    + "once Team C confirms POST /internal/v1/drive-rankings is live)");
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
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("rank", cs.getRank());
                entry.put("studentId", cs.getStudentId());
                entry.put("applicationId", cs.getApplicationId());
                entry.put("totalScore", cs.getTotalScore());
                candidates.add(entry);
            }
        }

        String rankingId = resolveRankingId(driveId, result, candidates);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("driveId", driveId);
        body.put("rankingId", rankingId);
        body.put("algorithm", result.getAlgorithm());
        body.put("graphVersion", result.getGraphVersion());
        body.put("tieBreakRule", result.getTieBreakRule());
        body.put("rankedCandidates", candidates);

        return objectMapper.writeValueAsString(body);
    }

    /**
     * Resolves the rankingId to include in the write-back payload.
     * If the RankingResult has an explicit custom rankingId (other than the default
     * placeholder "RANKING_READY" / "RANKING_EMPTY" / "RANKING_INVALID_WEIGHTS"),
     * it is preserved. Otherwise, a unique, stable rankingId is generated for the
     * distinct ranking run so no ID is reused across different payloads.
     */
    String resolveRankingId(String driveId, RankingResult result, List<Map<String, Object>> candidates) {
        if (result != null && result.getRankingId() != null
                && !result.getRankingId().isBlank()
                && !"RANKING_READY".equalsIgnoreCase(result.getRankingId())
                && !"RANKING_EMPTY".equalsIgnoreCase(result.getRankingId())
                && !"RANKING_INVALID_WEIGHTS".equalsIgnoreCase(result.getRankingId())) {
            return result.getRankingId();
        }

        return generateStableRankingId(driveId, result, candidates);
    }

    /**
     * Generates a stable, deterministic ranking ID based on the payload fields.
     * Identical ranking runs yield the identical ID (idempotent), while different
     * ranking payloads yield distinct IDs.
     */
    public static String generateStableRankingId(
            String driveId, RankingResult result, List<Map<String, Object>> candidates) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(driveId != null ? driveId : "").append("|");
            sb.append(result != null && result.getAlgorithm() != null ? result.getAlgorithm() : "").append("|");
            sb.append(result != null && result.getGraphVersion() != null ? result.getGraphVersion() : "").append("|");
            sb.append(result != null && result.getTieBreakRule() != null ? result.getTieBreakRule() : "").append("|");
            if (candidates != null) {
                for (Map<String, Object> c : candidates) {
                    sb.append(c.get("rank")).append(":")
                      .append(c.get("studentId")).append(":")
                      .append(c.get("applicationId")).append(":")
                      .append(c.get("totalScore")).append(";");
                }
            }
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            String prefix = (driveId != null && !driveId.isBlank()) ? driveId : "DRIVE";
            String algo = (result != null && result.getAlgorithm() != null && !result.getAlgorithm().isBlank())
                    ? result.getAlgorithm().toUpperCase()
                    : "RANK";
            return "RNK-" + prefix + "-" + algo + "-" + hex.toString();
        } catch (Exception e) {
            return "RNK-" + (driveId != null ? driveId : "DRIVE") + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
