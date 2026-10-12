package com.placement.teamb.ranking;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.RankingCandidate;

public class WeightedScoreRankingTest {

    public static void main(String[] args) {

        RankingCandidate candidate1 =
                new RankingCandidate(
                        "TEST_01",
                        Arrays.asList("Java", "SQL"),
                        8.5,
                        0,
                        80
                );

        RankingCandidate candidate2 =
                new RankingCandidate(
                        "TEST_02",
                        Arrays.asList("Python"),
                        7.5,
                        0,
                        70
                );

        List<RankingCandidate> candidates =
                Arrays.asList(candidate1, candidate2);

        RankingRequest request = new RankingRequest();

        request.setDriveId("DRIVE_FROM_TEAM_C");
        request.setCandidatePool(candidates);
        request.setGraphVersion("GRAPH_TEST_V1");
        request.setAlgorithm("WEIGHTED_SCORE");
        Map<String, Object> weights = new HashMap<>();

weights.put("cgpa", 0.4);
weights.put("skills", 0.4);
weights.put("aptitude", 0.2);

Map<String, Object> constraints = new HashMap<>();
constraints.put("weights", weights);

request.setConstraints(constraints);

        WeightedScoreRanking ranking =
                new WeightedScoreRanking();

        RankingResult result =
                ranking.computeRanking(request);

        System.out.println("===== RANKING TEST =====");

        for (var candidate : result.getOrderedCandidates()) {

            System.out.println(
                    "Student: "
                    + candidate.getStudentId()
                    + " | Score: "
                    + candidate.getTotalScore()
                    + " | Rank: "
                    + candidate.getRank()
            );
        }

        System.out.println(
                "Algorithm: " + result.getAlgorithm()
        );

        System.out.println(
                "Graph Version: " + result.getGraphVersion()
        );

        System.out.println(
                "Candidates Explored: "
                + result.getExploredCandidateCount()
        );
    }
}