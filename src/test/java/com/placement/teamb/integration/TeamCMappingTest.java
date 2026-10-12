package com.placement.teamb.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;
import com.placement.teamb.model.CandidateScore;
import com.placement.teamb.model.RankingCandidate;
import com.placement.teamb.ranking.HeapTopKRanking;
import com.placement.teamb.ranking.MergeSortRanking;
import com.placement.teamb.ranking.WeightedScoreRanking;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that:
 * 1. The Team C JSON response shapes (applications + students) parse correctly
 *    into our DTOs — using real JSON captured from the live API.
 * 2. Eligible student IDs are filtered correctly from applications JSON.
 * 3. RankingCandidate objects are built from parsed student records.
 * 4. All three ranking algorithms produce correct, ordered results when given
 *    the real candidate data.
 *
 * These are pure unit tests — no Spring context, no network calls.
 * The JSON literals below were captured from the live Team C API on 2026-10-12.
 */
class TeamCMappingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Real Team C JSON snippets (captured 2026-10-12) ─────────────────────

    /**
     * Minimal but structurally accurate excerpt of the real applications
     * response. Contains DRV001 ELIGIBLE, DRV002 ELIGIBLE, and one NOT_ELIGIBLE.
     */
    private static final String APPLICATIONS_JSON = """
            {
              "data": [
                {
                  "_id": "APP001",
                  "studentId": "STU001",
                  "driveId": "DRV001",
                  "eligibility": { "result": "ELIGIBLE" }
                },
                {
                  "_id": "APP002",
                  "studentId": "STU002",
                  "driveId": "DRV001",
                  "eligibility": { "result": "ELIGIBLE" }
                },
                {
                  "_id": "APP004",
                  "studentId": "STU004",
                  "driveId": "DRV001",
                  "eligibility": { "result": "ELIGIBLE" }
                },
                {
                  "_id": "APP005",
                  "studentId": "STU006",
                  "driveId": "DRV001",
                  "eligibility": { "result": "ELIGIBLE" }
                },
                {
                  "_id": "APP008",
                  "studentId": "STU012",
                  "driveId": "DRV001",
                  "eligibility": { "result": "ELIGIBLE" }
                },
                {
                  "_id": "APP007",
                  "studentId": "STU009",
                  "driveId": "DRV002",
                  "eligibility": { "result": "NOT_ELIGIBLE" }
                },
                {
                  "_id": "APP003",
                  "studentId": "STU003",
                  "driveId": "DRV002",
                  "eligibility": { "result": "ELIGIBLE" }
                }
              ],
              "meta": { "count": 7 }
            }
            """;

    /**
     * Structurally accurate excerpt of the real students response.
     * Uses the exact field names from the live API (_id, skills, academic.*).
     */
    private static final String STUDENTS_JSON = """
            {
              "data": [
                {
                  "_id": "STU001",
                  "name": "Aarav Patil",
                  "branch": "CSE",
                  "skills": ["Java", "Python", "MongoDB", "Node.js", "React"],
                  "academic": { "cgpa": 9.28, "backlogs": 0, "attendancePct": 92, "batch": 2027, "aptitudeScore": 0 }
                },
                {
                  "_id": "STU002",
                  "name": "Priya Sharma",
                  "branch": "IT",
                  "skills": ["Java", "SQL", "Python", "Web Development"],
                  "academic": { "cgpa": 8.65, "backlogs": 0, "attendancePct": 88, "batch": 2027, "aptitudeScore": 0 }
                },
                {
                  "_id": "STU004",
                  "name": "Sneha Deshmukh",
                  "branch": "CSE",
                  "skills": ["Python", "Machine Learning", "SQL", "Flask"],
                  "academic": { "cgpa": 8.92, "backlogs": 0, "attendancePct": 95, "batch": 2027, "aptitudeScore": 0 }
                },
                {
                  "_id": "STU006",
                  "name": "Ananya More",
                  "branch": "CSE",
                  "skills": ["Java", "Spring Boot", "MySQL", "Git"],
                  "academic": { "cgpa": 9.05, "backlogs": 0, "attendancePct": 90, "batch": 2027, "aptitudeScore": 0 }
                },
                {
                  "_id": "STU012",
                  "name": "Riya Patil",
                  "branch": "CSE",
                  "skills": ["Java", "React", "Node.js", "MongoDB"],
                  "academic": { "cgpa": 9.12, "backlogs": 0, "attendancePct": 93, "batch": 2027, "aptitudeScore": 0 }
                }
              ],
              "meta": { "count": 5 }
            }
            """;

    // ─── 1. JSON Parsing ──────────────────────────────────────────────────────

    @Test
    void applicationsJson_parsesCorrectly() throws Exception {
        TeamCApplicationsResponse response =
                mapper.readValue(APPLICATIONS_JSON, TeamCApplicationsResponse.class);

        assertThat(response.getData()).hasSize(7);

        TeamCApplicationsResponse.ApplicationRecord first = response.getData().get(0);
        assertThat(first.getId()).isEqualTo("APP001");
        assertThat(first.getStudentId()).isEqualTo("STU001");
        assertThat(first.getDriveId()).isEqualTo("DRV001");
        assertThat(first.getEligibility()).isNotNull();
        assertThat(first.getEligibility().getResult()).isEqualTo("ELIGIBLE");
    }

    @Test
    void studentsJson_parsesCorrectly() throws Exception {
        TeamCStudentsResponse response =
                mapper.readValue(STUDENTS_JSON, TeamCStudentsResponse.class);

        assertThat(response.getData()).hasSize(5);

        TeamCStudentsResponse.StudentRecord stu001 = response.getData().get(0);
        assertThat(stu001.getId()).isEqualTo("STU001");
        assertThat(stu001.getName()).isEqualTo("Aarav Patil");
        assertThat(stu001.getSkills()).containsExactlyInAnyOrder(
                "Java", "Python", "MongoDB", "Node.js", "React");

        TeamCStudentsResponse.Academic academic = stu001.getAcademic();
        assertThat(academic).isNotNull();
        assertThat(academic.getCgpa()).isEqualTo(9.28);
        assertThat(academic.getBacklogs()).isEqualTo(0);
        assertThat(academic.getAptitudeScore()).isEqualTo(0.0);
    }

    // ─── 2. Eligibility filtering ─────────────────────────────────────────────

    @Test
    void eligibilityFilter_onlyDrv001EligibleStudentsReturned() throws Exception {
        TeamCApplicationsResponse response =
                mapper.readValue(APPLICATIONS_JSON, TeamCApplicationsResponse.class);

        List<String> eligible = response.getData().stream()
                .filter(app -> "DRV001".equals(app.getDriveId()))
                .filter(app -> app.getEligibility() != null
                        && "ELIGIBLE".equalsIgnoreCase(app.getEligibility().getResult()))
                .map(TeamCApplicationsResponse.ApplicationRecord::getStudentId)
                .toList();

        // DRV001 has 5 ELIGIBLE students per live API data
        assertThat(eligible).hasSize(5);
        assertThat(eligible).containsExactlyInAnyOrder(
                "STU001", "STU002", "STU004", "STU006", "STU012");
    }

    @Test
    void eligibilityFilter_notEligibleStudentsExcluded() throws Exception {
        TeamCApplicationsResponse response =
                mapper.readValue(APPLICATIONS_JSON, TeamCApplicationsResponse.class);

        List<String> eligible = response.getData().stream()
                .filter(app -> "DRV002".equals(app.getDriveId()))
                .filter(app -> app.getEligibility() != null
                        && "ELIGIBLE".equalsIgnoreCase(app.getEligibility().getResult()))
                .map(TeamCApplicationsResponse.ApplicationRecord::getStudentId)
                .toList();

        // STU009 is NOT_ELIGIBLE; only STU003 is ELIGIBLE for DRV002
        assertThat(eligible).containsExactly("STU003");
        assertThat(eligible).doesNotContain("STU009");
    }

    // ─── 3. RankingCandidate construction from parsed student records ──────────

    @Test
    void rankingCandidates_builtCorrectlyFromStudentRecords() throws Exception {
        TeamCStudentsResponse response =
                mapper.readValue(STUDENTS_JSON, TeamCStudentsResponse.class);

        List<RankingCandidate> candidates = response.getData().stream()
                .map(rec -> {
                    TeamCStudentsResponse.Academic ac = rec.getAcademic();
                    double cgpa     = ac != null ? ac.getCgpa()         : 0.0;
                    int backlogs    = ac != null ? ac.getBacklogs()      : 0;
                    double aptitude = ac != null ? ac.getAptitudeScore() : 0.0;
                    List<String> skills = rec.getSkills() != null
                            ? rec.getSkills() : Collections.emptyList();
                    return new RankingCandidate(rec.getId(), skills, cgpa, backlogs, aptitude);
                })
                .toList();

        assertThat(candidates).hasSize(5);

        RankingCandidate stu001 = candidates.stream()
                .filter(c -> "STU001".equals(c.getStudentId()))
                .findFirst().orElseThrow();

        assertThat(stu001.getCgpa()).isEqualTo(9.28);
        assertThat(stu001.getBacklogs()).isEqualTo(0);
        assertThat(stu001.getAptitudeScore()).isEqualTo(0.0);
        assertThat(stu001.getSkills()).hasSize(5);
    }

    // ─── 4. End-to-end ranking with real DRV001 candidate data ───────────────

    private List<RankingCandidate> drv001Candidates() throws Exception {
        TeamCStudentsResponse response =
                mapper.readValue(STUDENTS_JSON, TeamCStudentsResponse.class);
        return response.getData().stream()
                .map(rec -> {
                    TeamCStudentsResponse.Academic ac = rec.getAcademic();
                    return new RankingCandidate(
                            rec.getId(),
                            rec.getSkills() != null ? rec.getSkills() : Collections.emptyList(),
                            ac != null ? ac.getCgpa() : 0.0,
                            ac != null ? ac.getBacklogs() : 0,
                            ac != null ? ac.getAptitudeScore() : 0.0
                    );
                })
                .toList();
    }

    @Test
    void mergeSort_drv001_stu001RanksFirst() throws Exception {
        /*
         * Score = cgpa + aptitude/10 + skillCount (aptitude is 0 for all DRV001 candidates)
         * STU001: 9.28 + 0 + 5 = 14.28  ← highest
         * STU012: 9.12 + 0 + 4 = 13.12
         * STU006: 9.05 + 0 + 4 = 13.05
         * STU004: 8.92 + 0 + 4 = 12.92
         * STU002: 8.65 + 0 + 4 = 12.65  ← lowest
         */
        List<RankingCandidate> candidates = drv001Candidates();
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "MERGE_SORT",
                "SCORE_DESCENDING", Map.of("topK", 5));

        RankingResult result = new MergeSortRanking().computeRanking(req);

        assertThat(result.getRankingId()).isEqualTo("RANKING_READY");
        assertThat(result.getAlgorithm()).isEqualTo("MERGE_SORT");
        assertThat(result.getOrderedCandidates()).hasSize(5);

        CandidateScore rank1 = result.getOrderedCandidates().get(0);
        assertThat(rank1.getStudentId()).isEqualTo("STU001");
        assertThat(rank1.getRank()).isEqualTo(1);
        assertThat(rank1.getTotalScore()).isEqualTo(9.28 + 5); // aptitude=0

        CandidateScore rank5 = result.getOrderedCandidates().get(4);
        assertThat(rank5.getStudentId()).isEqualTo("STU002");
        assertThat(rank5.getRank()).isEqualTo(5);
    }

    @Test
    void heapTopK_drv001_topK3_returnsThreeBestCandidates() throws Exception {
        List<RankingCandidate> candidates = drv001Candidates();
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "HEAP_TOPK",
                "SCORE_DESCENDING", Map.of("topK", 3));

        RankingResult result = new HeapTopKRanking().computeRanking(req);

        assertThat(result.getOrderedCandidates()).hasSize(3);
        assertThat(result.getOrderedCandidates().get(0).getStudentId()).isEqualTo("STU001");
        assertThat(result.getOrderedCandidates().get(1).getStudentId()).isEqualTo("STU012");
        assertThat(result.getOrderedCandidates().get(2).getStudentId()).isEqualTo("STU006");
    }

    @Test
    void weighted_drv001_cgpaHeavyWeighting_stu001RanksFirst() throws Exception {
        List<RankingCandidate> candidates = drv001Candidates();
        // cgpa-heavy weights: cgpa=8, skills=2, aptitude=1
        // (aptitude is 0 for everyone, so cgpa dominates)
        Map<String, Object> constraints = Map.of(
                "weights", Map.of("cgpa", 8.0, "skills", 2.0, "aptitude", 1.0));
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "WEIGHTED",
                "SCORE_DESCENDING", constraints);

        RankingResult result = new WeightedScoreRanking().computeRanking(req);

        assertThat(result.getRankingId()).isEqualTo("RANKING_READY");
        assertThat(result.getAlgorithm()).isEqualTo("WEIGHTED_SCORE");
        assertThat(result.getOrderedCandidates()).hasSize(5);
        // STU001 has highest CGPA (9.28) so must be rank 1
        assertThat(result.getOrderedCandidates().get(0).getStudentId()).isEqualTo("STU001");
        assertThat(result.getOrderedCandidates().get(0).getRank()).isEqualTo(1);
    }

    @Test
    void weighted_drv001_skillsHeavyWeighting_stu001StillFirst() throws Exception {
        // STU001 has 5 skills vs 4 for all others, so skills-heavy still puts them first
        List<RankingCandidate> candidates = drv001Candidates();
        Map<String, Object> constraints = Map.of(
                "weights", Map.of("cgpa", 1.0, "skills", 10.0, "aptitude", 0.0));
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "WEIGHTED",
                "SCORE_DESCENDING", constraints);

        RankingResult result = new WeightedScoreRanking().computeRanking(req);

        assertThat(result.getOrderedCandidates()).hasSize(5);
        assertThat(result.getOrderedCandidates().get(0).getStudentId()).isEqualTo("STU001");
    }

    // ─── 5. Rank assignment correctness ──────────────────────────────────────

    @Test
    void rankAssignment_isSequentialFrom1() throws Exception {
        List<RankingCandidate> candidates = drv001Candidates();
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "MERGE_SORT",
                "SCORE_DESCENDING", Map.of());

        RankingResult result = new MergeSortRanking().computeRanking(req);

        List<CandidateScore> ordered = result.getOrderedCandidates();
        for (int i = 0; i < ordered.size(); i++) {
            assertThat(ordered.get(i).getRank())
                    .as("rank at index %d", i)
                    .isEqualTo(i + 1);
        }
    }

    @Test
    void rankAssignment_scoresAreDescending() throws Exception {
        List<RankingCandidate> candidates = drv001Candidates();
        RankingRequest req = new RankingRequest(
                "DRV001", candidates, "TEAM-CURRENT", "MERGE_SORT",
                "SCORE_DESCENDING", Map.of());

        RankingResult result = new MergeSortRanking().computeRanking(req);

        List<CandidateScore> ordered = result.getOrderedCandidates();
        for (int i = 0; i < ordered.size() - 1; i++) {
            assertThat(ordered.get(i).getTotalScore())
                    .as("score at rank %d should be >= rank %d", i + 1, i + 2)
                    .isGreaterThanOrEqualTo(ordered.get(i + 1).getTotalScore());
        }
    }

    // ─── 6. Graph + SkillMatcher with real candidate data ────────────────────

    @Test
    void adjacencyMatrixGraph_storesAndRetrievesSkillsCorrectly() throws Exception {
        com.placement.teamb.graph.AdjacencyMatrixGraph graph =
                new com.placement.teamb.graph.AdjacencyMatrixGraph();

        List<RankingCandidate> candidates = drv001Candidates();
        for (RankingCandidate c : candidates) {
            graph.addStudent(c.getStudentId(), c.getSkills());
        }

        assertThat(graph.getStudentCount()).isEqualTo(5);
        // STU001 has 5 skills
        assertThat(graph.getStudentSkills("STU001")).hasSize(5);
        assertThat(graph.getStudentSkills("STU001")).contains("Java", "Python", "MongoDB");
        // STU002 has 4 skills
        assertThat(graph.getStudentSkills("STU002")).hasSize(4);
    }

    @Test
    void skillMatcher_matchesJavaSkillsFromRealCandidates() throws Exception {
        com.placement.teamb.matching.SkillMatcher matcher =
                new com.placement.teamb.matching.SkillMatcher();

        List<RankingCandidate> candidates = drv001Candidates();
        List<String> required = Arrays.asList("Java", "Spring Boot", "SQL");

        // STU001 has Java (1 match), STU006 has Java + Spring Boot (2 matches)
        RankingCandidate stu001 = candidates.stream()
                .filter(c -> "STU001".equals(c.getStudentId())).findFirst().orElseThrow();
        RankingCandidate stu006 = candidates.stream()
                .filter(c -> "STU006".equals(c.getStudentId())).findFirst().orElseThrow();

        assertThat(matcher.getMatchCount(stu001, required)).isEqualTo(1); // only Java
        assertThat(matcher.getMatchCount(stu006, required)).isEqualTo(2); // Java + Spring Boot
    }
}
