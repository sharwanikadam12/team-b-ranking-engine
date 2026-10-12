package com.placement.teamb.model;

import java.util.List;

/**
 * Represents a placement drive profile with required skills and cut-offs.
 */
public class DriveProfile {

    private String driveId;
    private List<String> requiredSkills;
    private double weightage;
    private double minCutoff;

    public DriveProfile() {}

    public DriveProfile(String driveId, List<String> requiredSkills,
                        double weightage, double minCutoff) {
        this.driveId = driveId;
        this.requiredSkills = requiredSkills;
        this.weightage = weightage;
        this.minCutoff = minCutoff;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public double getWeightage() { return weightage; }
    public void setWeightage(double weightage) { this.weightage = weightage; }

    public double getMinCutoff() { return minCutoff; }
    public void setMinCutoff(double minCutoff) { this.minCutoff = minCutoff; }
}