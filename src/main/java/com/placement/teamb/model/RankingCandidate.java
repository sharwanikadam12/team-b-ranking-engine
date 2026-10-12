package com.placement.teamb.model;

import java.util.List;

/**
 * Represents a candidate eligible for a placement drive, enriched with
 * academic and skill data fetched from Team C.
 */
public class RankingCandidate {

    private String studentId;
    private List<String> skills;
    private double cgpa;
    private int backlogs;
    private double aptitudeScore;

    public RankingCandidate() {}

    public RankingCandidate(String studentId, List<String> skills,
                            double cgpa, int backlogs, double aptitudeScore) {
        this.studentId = studentId;
        this.skills = skills;
        this.cgpa = cgpa;
        this.backlogs = backlogs;
        this.aptitudeScore = aptitudeScore;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

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
        return "RankingCandidate{studentId='" + studentId + "', cgpa=" + cgpa
                + ", backlogs=" + backlogs + ", aptitude=" + aptitudeScore
                + ", skills=" + skills + "}";
    }
}