package com.placement.teamb.integration;

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

        TeamCClient client = new TeamCClient(teamCUrl, 30L);

        EligibleApplicationService service =
                new EligibleApplicationService(client);

        String driveId = args.length > 0 ? args[0] : "DRV001";

        List<String> students =
                service.getEligibleStudentIds(driveId);

        System.out.println("===== TEAM C ELIGIBLE STUDENTS FOR " + driveId + " =====");

        for (String studentId : students) {
            System.out.println(studentId);
        }

        System.out.println("Total: " + students.size());
    }
}