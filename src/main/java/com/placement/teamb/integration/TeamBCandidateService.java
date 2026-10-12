package com.placement.teamb.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.placement.teamb.model.RankingCandidate;

/**
 * Builds the list of {@link RankingCandidate} objects for a given drive by:
 *
 * 1. Fetching eligible student IDs from Team C's applications endpoint
 *    (via {@link EligibleApplicationService}).
 * 2. Fetching all student records from Team C's students endpoint.
 * 3. Joining the two sets on studentId to build enriched RankingCandidate objects.
 *
 * Team C's students response (verified 2026-10-11):
 * {
 *   "data": [
 *     { "_id": "STU001", "skills": [...],
 *       "academic": { "cgpa": 9.28, "backlogs": 0, "aptitudeScore": 0 } }
 *   ]
 * }
 *
 * NOTE: aptitudeScore is currently 0 for all students in Team C's seed data.
 * Ranking algorithms handle 0-aptitude gracefully.
 */
@Service
public class TeamBCandidateService {

    private static final Logger log =
            LoggerFactory.getLogger(TeamBCandidateService.class);

    private final TeamCClient teamCClient;
    private final EligibleApplicationService eligibleApplicationService;
    private final ObjectMapper objectMapper;

    public TeamBCandidateService(
            TeamCClient teamCClient,
            EligibleApplicationService eligibleApplicationService) {

        this.teamCClient = teamCClient;
        this.eligibleApplicationService = eligibleApplicationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns a list of enriched {@link RankingCandidate} objects for all
     * students that are eligible for the given drive.
     *
     * @param driveId the drive ID (e.g. "DRV001")
     * @return non-null list; may be empty if no eligible candidates found
     * @throws TeamCException if Team C is unreachable or its response is invalid
     */
    public List<RankingCandidate> getEligibleCandidates(String driveId) {

        // Step 1: Get eligible student IDs
        List<String> eligibleStudentIds =
                eligibleApplicationService.getEligibleStudentIds(driveId);

        if (eligibleStudentIds.isEmpty()) {
            log.info("No eligible students found for drive {}", driveId);
            return Collections.emptyList();
        }

        // Step 2: Fetch all students and build a lookup map
        Map<String, TeamCStudentsResponse.StudentRecord> studentMap =
                fetchStudentMap();

        // Step 3: Join
        List<RankingCandidate> candidates = new ArrayList<>();

        for (String studentId : eligibleStudentIds) {

            TeamCStudentsResponse.StudentRecord record =
                    studentMap.get(studentId);

            if (record == null) {
                log.warn("Student {} is eligible for {} but not found in "
                        + "Team C /api/v1/students — skipping",
                        studentId, driveId);
                continue;
            }

            TeamCStudentsResponse.Academic academic = record.getAcademic();

            double cgpa = academic != null ? academic.getCgpa() : 0.0;
            int backlogs = academic != null ? academic.getBacklogs() : 0;
            double aptitude = academic != null ? academic.getAptitudeScore() : 0.0;
            List<String> skills = record.getSkills() != null
                    ? record.getSkills()
                    : Collections.emptyList();

            candidates.add(new RankingCandidate(
                    studentId, skills, cgpa, backlogs, aptitude));

            log.info("Candidate added: {} | CGPA={} backlogs={} aptitude={} skills={}",
                    studentId, cgpa, backlogs, aptitude, skills);
        }

        log.info("Built {} RankingCandidate(s) for drive {}", candidates.size(), driveId);
        return candidates;
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private Map<String, TeamCStudentsResponse.StudentRecord> fetchStudentMap() {

        String rawJson;
        try {
            rawJson = teamCClient.getStudents();
        } catch (TeamCException e) {
            log.error("Failed to fetch students from Team C: {}", e.getMessage());
            throw e;
        }

        TeamCStudentsResponse response;
        try {
            response = objectMapper.readValue(
                    rawJson, TeamCStudentsResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Team C students response: {}",
                    e.getMessage());
            throw new TeamCException(
                    "Team C students response could not be parsed", e);
        }

        if (response.getData() == null) {
            log.warn("Team C returned null data array for students");
            return Collections.emptyMap();
        }

        Map<String, TeamCStudentsResponse.StudentRecord> map = new HashMap<>();

        for (TeamCStudentsResponse.StudentRecord record : response.getData()) {
            if (record != null && record.getId() != null) {
                map.put(record.getId(), record);
            }
        }

        log.info("Loaded {} student records from Team C", map.size());
        return map;
    }
}