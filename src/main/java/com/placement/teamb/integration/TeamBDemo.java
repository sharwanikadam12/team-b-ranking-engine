package com.placement.teamb.integration;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.RankingCandidate;
import com.placement.teamb.ranking.MergeSortRanking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Standalone CLI demo for Team B's ranking pipeline.
 *
 * Run directly with: java -cp target/... com.placement.teamb.integration.TeamBDemo
 * (Not a Spring component — does not wire any Spring beans.)
 *
 * NOTE: This demo requires TEAM_C_BASE_URL environment variable to be set,
 *       or it defaults to the deployed Team C URL.
 */
public class TeamBDemo {

    public static void main(String[] args) {

        String teamCUrl = System.getenv("TEAM_C_BASE_URL");
        if (teamCUrl == null || teamCUrl.isBlank()) {
            teamCUrl = "https://rit-placement-team-c-backend.onrender.com";
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("======================================");
        System.out.println("       TEAM B PLACEMENT RANKING");
        System.out.println("======================================");
        System.out.println("\nTeam C URL: " + teamCUrl);
        System.out.println("\nAvailable Drives (from Team C):");
        System.out.println("1. DRV001 — TechCorp Solutions");
        System.out.println("2. DRV002 — Innovate Systems");
        System.out.println("3. DRV003 — DataSoft Technologies");

        System.out.print("\nEnter Drive ID: ");
        String driveId = scanner.nextLine().trim();

        // Pass empty write-token — this CLI demo only reads data, never POSTs.
        TeamCClient teamCClient = new TeamCClient(teamCUrl, 30L, "");

        EligibleApplicationService eligibleService =
                new EligibleApplicationService(teamCClient);

        TeamBCandidateService candidateService =
                new TeamBCandidateService(teamCClient, eligibleService);

        System.out.println("\nDrive: " + driveId);
        System.out.println("Reading eligible candidates from Team C...");

        List<RankingCandidate> candidates =
                candidateService.getEligibleCandidates(driveId);

        System.out.println("Eligible candidates found: " + candidates.size());

        if (candidates.isEmpty()) {
            System.out.println(
                    "\nNo eligible candidates found for " + driveId);
            System.out.println("Please check Team C applications.");
            scanner.close();
            return;
        }

        System.out.println("\nCandidate Pool:");

        for (RankingCandidate candidate : candidates) {
            System.out.println(
                    candidate.getStudentId()
                    + " | CGPA: " + candidate.getCgpa()
                    + " | Backlogs: " + candidate.getBacklogs()
                    + " | Aptitude: " + candidate.getAptitudeScore()
                    + " | Skills: " + candidate.getSkills()
            );
        }

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("topK", candidates.size());

        RankingRequest request = new RankingRequest(
                driveId,
                candidates,
                "TEAM-CURRENT",
                "MERGE_SORT",
                "SCORE_DESCENDING",
                constraints
        );

        MergeSortRanking ranking = new MergeSortRanking();
        RankingResult result = ranking.computeRanking(request);

        System.out.println("\n======================================");
        System.out.println("             RANKING RESULT");
        System.out.println("======================================");
        System.out.println("Status: " + result.getRankingId());
        System.out.println("Drive: " + driveId);
        System.out.println("Algorithm: " + result.getAlgorithm());
        System.out.println("Candidates explored: " + result.getExploredCandidateCount());
        System.out.println("\nFinal Ranking:");

        result.getOrderedCandidates().forEach(candidate ->
                System.out.println(
                        "Rank " + candidate.getRank()
                        + " -> " + candidate.getStudentId()
                        + " | Score: " + candidate.getTotalScore()
                )
        );

        System.out.println("\n======================================");
        System.out.println("        TEAM B DEMO COMPLETED");
        System.out.println("======================================");

        scanner.close();
    }
}