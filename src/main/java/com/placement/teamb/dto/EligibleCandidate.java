package com.placement.teamb.dto;

/**
 * Lightweight DTO that pairs a student ID with the drive they are eligible for.
 * Used internally to carry eligible student IDs fetched from Team C's
 * /api/v1/applications endpoint before we enrich them with student details.
 */
public class EligibleCandidate {

    private String driveId;
    private String studentId;

    public EligibleCandidate() {}

    public EligibleCandidate(String driveId, String studentId) {
        this.driveId = driveId;
        this.studentId = studentId;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}
