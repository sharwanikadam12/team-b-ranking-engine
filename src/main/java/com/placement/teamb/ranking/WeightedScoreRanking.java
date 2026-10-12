package com.placement.teamb.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import com.placement.teamb.model.RankingCandidate;

@Service
public class WeightedScoreRanking implements RankingService {

    @Override
    public RankingResult computeRanking(RankingRequest request) {

        List<CandidateScore> scores = new ArrayList<>();

        if (request == null || request.getCandidatePool() == null) {

            return new RankingResult(
                    "RANKING_EMPTY",
                    scores,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "WEIGHTED_SCORE",
                    request != null ? request.getGraphVersion() : null,
                    0
            );
        }

        Map<String, Object> constraints =
                request.getConstraints();

        if (constraints == null
                || !constraints.containsKey("weights")) {

            return new RankingResult(
                    "RANKING_INVALID_WEIGHTS",
                    scores,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "WEIGHTED_SCORE",
                    request.getGraphVersion(),
                    0
            );
        }

        Object weightsObject =
                constraints.get("weights");

        if (!(weightsObject instanceof Map)) {

            return new RankingResult(
                    "RANKING_INVALID_WEIGHTS",
                    scores,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "WEIGHTED_SCORE",
                    request.getGraphVersion(),
                    0
            );
        }

        Map<?, ?> weights =
                (Map<?, ?>) weightsObject;

        double cgpaWeight =
                getWeight(weights, "cgpa");

        double skillsWeight =
                getWeight(weights, "skills");

        double aptitudeWeight =
                getWeight(weights, "aptitude");

        double totalWeight =
                cgpaWeight
                + skillsWeight
                + aptitudeWeight;

        if (totalWeight <= 0) {

            return new RankingResult(
                    "RANKING_INVALID_WEIGHTS",
                    scores,
                    0.0,
                    "Higher score first, student ID as tie-break",
                    "WEIGHTED_SCORE",
                    request.getGraphVersion(),
                    0
            );
        }

        for (RankingCandidate candidate :
                request.getCandidatePool()) {

            if (candidate == null) {
                continue;
            }

            /*
             * Normalize CGPA approximately to a 10-point scale
             * and aptitude to a 100-point scale.
             *
             * Skill score is based on the number of skills
             * available in the candidate profile.
             *
             * The weights themselves come from the request.
             */

            double cgpaScore =
                    (candidate.getCgpa() / 10.0) * cgpaWeight;

            double aptitudeScore =
                    (candidate.getAptitudeScore() / 100.0)
                    * aptitudeWeight;

            double skillScore =
                    0.0;

            if (candidate.getSkills() != null) {
                skillScore =
                        candidate.getSkills().size()
                        * skillsWeight;
            }

            double totalScore =
                    cgpaScore
                    + aptitudeScore
                    + skillScore;

            scores.add(
                    new CandidateScore(
                            candidate.getStudentId(),
                            totalScore,
                            0
                    )
            );
        }

        scores.sort(
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

        for (CandidateScore score : scores) {
            score.setRank(rank++);
        }

        double totalScore = 0.0;

        for (CandidateScore score : scores) {
            totalScore += score.getTotalScore();
        }

        return new RankingResult(
                "RANKING_READY",
                scores,
                totalScore,
                "Higher score first, student ID as tie-break",
                "WEIGHTED_SCORE",
                request.getGraphVersion(),
                scores.size()
        );
    }

    private double getWeight(
            Map<?, ?> weights,
            String key) {

        Object value =
                weights.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return 0.0;
    }
}