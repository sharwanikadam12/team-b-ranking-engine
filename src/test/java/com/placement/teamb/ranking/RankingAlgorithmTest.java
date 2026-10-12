package com.placement.teamb.ranking;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.RankingCandidate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the three ranking algorithms.
 * These tests are self-contained — no Spring context, no network calls.
 */
class RankingAlgorithmTest {

    private final MergeSortRanking mergeSort = new MergeSortRanking();
    private final HeapTopKRanking heapTopK = new HeapTopKRanking();
    private final WeightedScoreRanking weighted = new WeightedScoreRanking();

    // ── helpers ──────────────────────────────────────────────────────────────

    private RankingCandidate candidate(String id, double cgpa,
                                       int backlogs, double aptitude,
                                       String... skills) {
        return new RankingCandidate(id, Arrays.asList(skills),
                cgpa, backlogs, aptitude);
    }

    private RankingRequest request(String driveId,
                                   List<RankingCandidate> pool,
                                   String algorithm,
                                   Map<String, Object> constraints) {
        return new RankingRequest(driveId, pool, "TEST-GRAPH",
                algorithm, "SCORE_DESCENDING", constraints);
    }

    // ── MergeSort ─────────────────────────────────────────────────────────────

    @Test
    void mergeSort_sortsHighCgpaFirst() {
        List<RankingCandidate> pool = Arrays.asList(
                candidate("STU002", 7.5, 0, 0.0, "Java"),
                candidate("STU001", 9.0, 0, 0.0, "Java", "Python")
        );
        RankingResult result = mergeSort.computeRanking(
                request("DRV001", pool, "MERGE_SORT", Map.of("topK", 2)));

        assertThat(result.getOrderedCandidates()).hasSize(2);
        assertThat(result.getOrderedCandidates().get(0).getStudentId())
                .isEqualTo("STU001");
        assertThat(result.getOrderedCandidates().get(0).getRank()).isEqualTo(1);
        assertThat(result.getOrderedCandidates().get(1).getRank()).isEqualTo(2);
    }

    @Test
    void mergeSort_emptyPool_returnsEmptyResult() {
        RankingResult result = mergeSort.computeRanking(
                request("DRV001", Collections.emptyList(), "MERGE_SORT", Map.of()));
        assertThat(result.getOrderedCandidates()).isEmpty();
        assertThat(result.getRankingId()).isEqualTo("RANKING_READY");
    }

    @Test
    void mergeSort_nullRequest_returnsEmptyResult() {
        RankingResult result = mergeSort.computeRanking(null);
        assertThat(result.getRankingId()).isEqualTo("RANKING_EMPTY");
    }

    // ── HeapTopK ──────────────────────────────────────────────────────────────

    @Test
    void heapTopK_topK1_returnsOnlyBestCandidate() {
        List<RankingCandidate> pool = Arrays.asList(
                candidate("STU003", 6.5, 1, 50.0, "C"),
                candidate("STU004", 9.2, 0, 80.0, "Java", "Python", "SQL"),
                candidate("STU005", 8.1, 0, 60.0, "Java")
        );
        Map<String, Object> constraints = Map.of("topK", 1);
        RankingResult result = heapTopK.computeRanking(
                request("DRV001", pool, "HEAP_TOPK", constraints));

        assertThat(result.getOrderedCandidates()).hasSize(1);
        assertThat(result.getOrderedCandidates().get(0).getStudentId())
                .isEqualTo("STU004");
    }

    // ── WeightedScore ─────────────────────────────────────────────────────────

    @Test
    void weighted_respectsWeights() {
        List<RankingCandidate> pool = Arrays.asList(
                candidate("STU_A", 8.0, 0, 80.0, "Java"),   // high aptitude
                candidate("STU_B", 9.5, 0, 10.0)            // high CGPA
        );
        Map<String, Object> constraints = Map.of(
                "weights", Map.of("cgpa", 5.0, "aptitude", 5.0, "skills", 1.0)
        );
        RankingResult result = weighted.computeRanking(
                request("DRV001", pool, "WEIGHTED", constraints));

        assertThat(result.getOrderedCandidates()).hasSize(2);
        // STU_B: (9.5/10)*5 + (10/100)*5 + 0*1 = 4.75 + 0.5 = 5.25
        // STU_A: (8.0/10)*5 + (80/100)*5 + 1*1 = 4.0  + 4.0 + 1 = 9.0
        assertThat(result.getOrderedCandidates().get(0).getStudentId())
                .isEqualTo("STU_A");
    }

    @Test
    void weighted_missingWeights_returnsInvalidResult() {
        List<RankingCandidate> pool = List.of(
                candidate("STU1", 8.0, 0, 0.0));
        RankingResult result = weighted.computeRanking(
                request("DRV001", pool, "WEIGHTED", Map.of()));
        assertThat(result.getRankingId()).isEqualTo("RANKING_INVALID_WEIGHTS");
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    @Test
    void dispatcher_defaultsToMergeSort() {
        MergeSortRanking ms = new MergeSortRanking();
        HeapTopKRanking hk = new HeapTopKRanking();
        WeightedScoreRanking ws = new WeightedScoreRanking();
        RankingDispatcher dispatcher = new RankingDispatcher(ms, hk, ws);

        List<RankingCandidate> pool = List.of(
                candidate("STU1", 8.0, 0, 0.0, "Java"));

        RankingRequest req = new RankingRequest(
                "DRV001", pool, "TEST", null, "SCORE_DESCENDING", Map.of());

        RankingResult result = dispatcher.computeRanking(req);
        assertThat(result.getAlgorithm()).isEqualTo("MERGE_SORT");
    }
}
