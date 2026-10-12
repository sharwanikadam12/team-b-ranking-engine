package com.placement.teamb.model;

/**
 * Represents a single eligibility rule for a drive.
 */
public class DriveRequirement {

    private String ruleId;
    private String ruleType;
    private Object threshold;
    private double weight;

    public DriveRequirement() {}

    public DriveRequirement(String ruleId, String ruleType,
                            Object threshold, double weight) {
        this.ruleId = ruleId;
        this.ruleType = ruleType;
        this.threshold = threshold;
        this.weight = weight;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Object getThreshold() { return threshold; }
    public void setThreshold(Object threshold) { this.threshold = threshold; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}