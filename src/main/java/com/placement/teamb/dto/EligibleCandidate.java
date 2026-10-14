package com.placement.teamb.dto;

/**
 * Lightweight DTO that pairs a student ID with the drive they are eligible for,
 * and now also carries the application's own ID ({@code _id} from Team C's
 * /api/v1/applications response).
 *
 * Used internally to carry eligible candidate data fetched from Team C before
 * it is enriched with student details and passed to the ranking pipeline.
 */
public class EligibleCandidate {

    private String driveId;
    private String studentId;
    /** The application's own ID (Team C {@code _id}, e.g. "APP001"). May be null for test data. */
    private String applicationId;

    public EligibleCandidate() {}

    /** Legacy 2-arg constructor — kept for backward compatibility; applicationId defaults to null. */
    public EligibleCandidate(String driveId, String studentId) {
        this.driveId = driveId;
        this.studentId = studentId;
    }

    /** Full constructor carrying the application ID from Team C's response. */
    public EligibleCandidate(String driveId, String studentId, String applicationId) {
        this.driveId = driveId;
        this.studentId = studentId;
        this.applicationId = applicationId;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
}
