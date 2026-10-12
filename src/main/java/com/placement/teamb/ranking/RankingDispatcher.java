package com.placement.teamb.ranking;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Dispatches ranking requests to the correct algorithm implementation
 * based on {@link RankingRequest#getAlgorithm()}.
 *
 * Supported algorithms:
 *   MERGE_SORT   → {@link MergeSortRanking}  (default)
 *   HEAP_TOPK    → {@link HeapTopKRanking}
 *   WEIGHTED     → {@link WeightedScoreRanking}
 *
 * This class resolves the Spring ambiguity that arises from having three
 * beans that all implement {@link RankingService}.
 */
@Service
public class RankingDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(RankingDispatcher.class);

    private final MergeSortRanking mergeSortRanking;
    private final HeapTopKRanking heapTopKRanking;
    private final WeightedScoreRanking weightedScoreRanking;

    public RankingDispatcher(
            MergeSortRanking mergeSortRanking,
            HeapTopKRanking heapTopKRanking,
            WeightedScoreRanking weightedScoreRanking) {

        this.mergeSortRanking = mergeSortRanking;
        this.heapTopKRanking = heapTopKRanking;
        this.weightedScoreRanking = weightedScoreRanking;
    }

    /**
     * Selects the correct algorithm and runs ranking.
     *
     * @param request the ranking request containing the algorithm name and candidate pool
     * @return the ranking result
     */
    public RankingResult computeRanking(RankingRequest request) {

        if (request == null) {
            log.warn("computeRanking called with null request");
            return emptyResult(null);
        }

        if (request.getCandidatePool() == null
                || request.getCandidatePool().isEmpty()) {
            log.info("No candidates in pool — returning empty result");
            return emptyResult(request);
        }

        String algorithm = request.getAlgorithm();

        if (algorithm == null) {
            algorithm = "MERGE_SORT";
        }

        log.info("Dispatching ranking for drive {} | algorithm={} | candidates={}",
                request.getDriveId(),
                algorithm,
                request.getCandidatePool().size());

        RankingResult result = switch (algorithm.toUpperCase()) {
            case "HEAP_TOPK" -> heapTopKRanking.computeRanking(request);
            case "WEIGHTED", "WEIGHTED_SCORE" -> weightedScoreRanking.computeRanking(request);
            default -> mergeSortRanking.computeRanking(request);
        };

        log.info("Ranking complete for drive {} | ranked {} candidates",
                request.getDriveId(),
                result.getOrderedCandidates() != null
                        ? result.getOrderedCandidates().size() : 0);

        return result;
    }

    private RankingResult emptyResult(RankingRequest request) {
        return new RankingResult(
                "RANKING_EMPTY",
                Collections.emptyList(),
                0.0,
                "Higher score first, student ID as tie-break",
                request != null ? request.getAlgorithm() : "MERGE_SORT",
                request != null ? request.getGraphVersion() : null,
                0
        );
    }
}
