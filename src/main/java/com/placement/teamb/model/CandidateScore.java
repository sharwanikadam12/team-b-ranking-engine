package com.placement.teamb.model;

/**
 * Holds the computed score and rank for a candidate after ranking.
 */
public class CandidateScore {

    private String studentId;
    private double totalScore;
    private int rank;

    public CandidateScore() {}

    public CandidateScore(String studentId, double totalScore, int rank) {
        this.studentId = studentId;
        this.totalScore = totalScore;
        this.rank = rank;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    @Override
    public String toString() {
        return "CandidateScore{rank=" + rank + ", studentId='" + studentId
                + "', score=" + totalScore + "}";
    }
}
