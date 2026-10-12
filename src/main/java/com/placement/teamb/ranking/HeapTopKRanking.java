package com.placement.teamb.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import com.placement.teamb.model.RankingCandidate;

@Service
public class HeapTopKRanking implements RankingService {

    @Override
    public RankingResult computeRanking(RankingRequest request) {

        List<CandidateScore> result = new ArrayList<>();

        if (request == null || request.getCandidatePool() == null) {
            return new RankingResult(
                    "RANKING_EMPTY",
                    result,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "HEAP_TOPK",
                    request != null ? request.getGraphVersion() : null,
                    0
            );
        }

        int k = request.getCandidatePool().size();

        if (request.getConstraints() != null
                && request.getConstraints().get("topK") instanceof Number) {

            k = ((Number)
                    request.getConstraints()
                            .get("topK"))
                    .intValue();
        }

        if (k <= 0) {
            k = request.getCandidatePool().size();
        }

        Comparator<CandidateScore> worstFirst =
                Comparator
                        .comparingDouble(
                                CandidateScore::getTotalScore
                        )
                        .thenComparing(
                                CandidateScore::getStudentId,
                                Comparator.reverseOrder()
                        );

        PriorityQueue<CandidateScore> heap =
                new PriorityQueue<>(
                        Math.max(1, k),
                        worstFirst
                );

        for (RankingCandidate candidate :
                request.getCandidatePool()) {

            if (candidate == null) {
                continue;
            }

            double score =
                    candidate.getCgpa()
                    + (candidate.getAptitudeScore() / 10.0)
                    + candidate.getSkills().size();

            CandidateScore candidateScore =
                    new CandidateScore(
                            candidate.getStudentId(),
                            score,
                            0
                    );

            heap.offer(candidateScore);

            if (heap.size() > k) {
                heap.poll();
            }
        }

        result.addAll(heap);

        result.sort(
                Comparator
                        .comparingDouble(
                                CandidateScore::getTotalScore
                        )
                        .reversed()
                        .thenComparing(
                                CandidateScore::getStudentId
                        )
        );

        int rank = 1;

        for (CandidateScore score : result) {
            score.setRank(rank++);
        }

        return new RankingResult(
                "RANKING_READY",
                result,
                result.stream()
                        .mapToDouble(
                                CandidateScore::getTotalScore
                        )
                        .sum(),
                "Higher score first, student ID as tie-break",
                "HEAP_TOPK",
                request.getGraphVersion(),
                request.getCandidatePool().size()
        );
    }
}