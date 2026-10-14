package com.placement.teamb.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import com.placement.teamb.model.RankingCandidate;

@Service
public class MergeSortRanking implements RankingService {

    @Override
    public RankingResult computeRanking(RankingRequest request) {

        List<CandidateScore> scores = new ArrayList<>();

        if (request == null || request.getCandidatePool() == null) {
            return new RankingResult(
                    "RANKING_EMPTY",
                    scores,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "MERGE_SORT",
                    request != null ? request.getGraphVersion() : null,
                    0
            );
        }

        for (RankingCandidate candidate : request.getCandidatePool()) {

            if (candidate == null) {
                continue;
            }

            double score =
                    candidate.getCgpa()
                    + (candidate.getAptitudeScore() / 10.0)
                    + candidate.getSkills().size();

            scores.add(
                    new CandidateScore(
                            candidate.getStudentId(),
                            candidate.getApplicationId(),
                            score,
                            0
                    )
            );
        }

        scores = mergeSort(scores);

        int rank = 1;

        for (CandidateScore score : scores) {
            score.setRank(rank++);
        }

        return new RankingResult(
                "RANKING_READY",
                scores,
                scores.stream()
                        .mapToDouble(CandidateScore::getTotalScore)
                        .sum(),
                "Higher score first, student ID as tie-break",
                "MERGE_SORT",
                request.getGraphVersion(),
                scores.size()
        );
    }

    private List<CandidateScore> mergeSort(
            List<CandidateScore> list) {

        if (list.size() <= 1) {
            return list;
        }

        int middle = list.size() / 2;

        List<CandidateScore> left =
                mergeSort(
                        new ArrayList<>(
                                list.subList(0, middle)
                        )
                );

        List<CandidateScore> right =
                mergeSort(
                        new ArrayList<>(
                                list.subList(middle, list.size())
                        )
                );

        return merge(left, right);
    }

    private List<CandidateScore> merge(
            List<CandidateScore> left,
            List<CandidateScore> right) {

        List<CandidateScore> result =
                new ArrayList<>();

        Comparator<CandidateScore> comparator =
                Comparator
                        .comparingDouble(
                                CandidateScore::getTotalScore
                        )
                        .reversed()
                        .thenComparing(
                                CandidateScore::getStudentId
                        );

        int i = 0;
        int j = 0;

        while (i < left.size() && j < right.size()) {

            if (comparator.compare(
                    left.get(i),
                    right.get(j)) <= 0) {

                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }

        while (i < left.size()) {
            result.add(left.get(i++));
        }

        while (j < right.size()) {
            result.add(right.get(j++));
        }

        return result;
    }
}