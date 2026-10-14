package com.placement.teamb.model;

/**
 * Holds the computed score and rank for a candidate after ranking.
 * {@code applicationId} carries the Team C application record ID ({@code _id})
 * so the write-back payload can include it alongside studentId and rank.
 */
public class CandidateScore {

    private String studentId;
    /** Team C application ID (e.g. "APP001"). Null when built from manual test data. */
    private String applicationId;
    private double totalScore;
    private int rank;

    public CandidateScore() {}

    /**
     * Backward-compatible 3-arg constructor retained so existing callers that
     * do not yet carry an applicationId continue to compile without change.
     * Sets {@code applicationId} to {@code null}.
     */
    public CandidateScore(String studentId, double totalScore, int rank) {
        this.studentId = studentId;
        this.applicationId = null;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    /**
     * Full 4-arg constructor used by ranking algorithms that receive a
     * {@link com.placement.teamb.model.RankingCandidate} carrying an applicationId.
     */
    public CandidateScore(String studentId, String applicationId,
                          double totalScore, int rank) {
        this.studentId = studentId;
        this.applicationId = applicationId;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    @Override
    public String toString() {
        return "CandidateScore{rank=" + rank + ", studentId='" + studentId
                + "', applicationId='" + applicationId
                + "', score=" + totalScore + "}";
    }
}
