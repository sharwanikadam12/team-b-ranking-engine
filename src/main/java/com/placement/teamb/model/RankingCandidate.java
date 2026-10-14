package com.placement.teamb.model;

import java.util.List;

/**
 * Represents a candidate eligible for a placement drive, enriched with
 * academic and skill data fetched from Team C.
 *
 * {@code applicationId} carries the Team C application record ID ({@code _id},
 * e.g. "APP001"). It is {@code null} when a candidate is constructed from
 * manual/test data using the legacy 5-arg constructor.
 */
public class RankingCandidate {

    private String studentId;
    /** Team C application ID. Null for manually constructed test candidates. */
    private String applicationId;
    private List<String> skills;
    private double cgpa;
    private int backlogs;
    private double aptitudeScore;

    public RankingCandidate() {}

    /**
     * Legacy 5-arg constructor — kept unchanged so all existing tests compile
     * without modification. Sets {@code applicationId} to {@code null}.
     */
    public RankingCandidate(String studentId, List<String> skills,
                            double cgpa, int backlogs, double aptitudeScore) {
        this.studentId = studentId;
        this.applicationId = null;
        this.skills = skills;
        this.cgpa = cgpa;
        this.backlogs = backlogs;
        this.aptitudeScore = aptitudeScore;
    }

    /**
     * Full 6-arg constructor used by {@link com.placement.teamb.integration.TeamBCandidateService}
     * when building candidates from a live Team C response that includes the application ID.
     */
    public RankingCandidate(String studentId, String applicationId,
                            List<String> skills, double cgpa,
                            int backlogs, double aptitudeScore) {
        this.studentId = studentId;
        this.applicationId = applicationId;
        this.skills = skills;
        this.cgpa = cgpa;
        this.backlogs = backlogs;
        this.aptitudeScore = aptitudeScore;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public int getBacklogs() { return backlogs; }
    public void setBacklogs(int backlogs) { this.backlogs = backlogs; }

    public double getAptitudeScore() { return aptitudeScore; }
    public void setAptitudeScore(double aptitudeScore) { this.aptitudeScore = aptitudeScore; }

    @Override
    public String toString() {
        return "RankingCandidate{studentId='" + studentId
                + "', applicationId='" + applicationId
                + "', cgpa=" + cgpa
                + ", backlogs=" + backlogs + ", aptitude=" + aptitudeScore
                + ", skills=" + skills + "}";
    }
}