package com.placement.teamb.integration;

import com.placement.teamb.dto.EligibleCandidate;
import java.util.List;

/**
 * Standalone CLI test for EligibleApplicationService.
 *
 * Run directly (not via Spring context):
 *   java -cp target/... com.placement.teamb.integration.EligibleApplicationTest
 */
public class EligibleApplicationTest {

    public static void main(String[] args) {

        String teamCUrl = System.getenv("TEAM_C_BASE_URL");
        if (teamCUrl == null || teamCUrl.isBlank()) {
            teamCUrl = "https://rit-placement-team-c-backend.onrender.com";
        }

        // Pass empty write-token — this CLI tool only reads data, never POSTs.
        TeamCClient client = new TeamCClient(teamCUrl, 30L, "");

        EligibleApplicationService service =
                new EligibleApplicationService(client);

        String driveId = args.length > 0 ? args[0] : "DRV001";

        List<EligibleCandidate> candidates =
                service.getEligibleApplications(driveId);

        System.out.println("===== TEAM C ELIGIBLE CANDIDATES FOR " + driveId + " =====");

        for (EligibleCandidate c : candidates) {
            System.out.println(c.getApplicationId() + " -> " + c.getStudentId());
        }

        System.out.println("Total: " + candidates.size());
    }
}