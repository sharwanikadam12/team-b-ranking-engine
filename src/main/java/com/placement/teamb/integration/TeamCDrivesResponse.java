package com.placement.teamb.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps Team C's GET /api/v1/drives response.
 *
 * Actual shape (verified 2026-10-11):
 * {
 *   "data": [
 *     { "_id":"DRV001", "company":"TechCorp Solutions", "package":1200000,
 *       "seats":10, "state":"ACTIVE",
 *       "ruleSet": { "version":"v1.0", "rules":[...] } }
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamCDrivesResponse {

    @JsonProperty("data")
    private List<DriveRecord> data;

    public List<DriveRecord> getData() { return data; }
    public void setData(List<DriveRecord> data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DriveRecord {

        @JsonProperty("_id")
        private String id;

        @JsonProperty("company")
        private String company;

        @JsonProperty("package")
        private long packageAmount;

        @JsonProperty("seats")
        private int seats;

        @JsonProperty("state")
        private String state;

        @JsonProperty("ruleSet")
        private RuleSet ruleSet;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }

        public long getPackageAmount() { return packageAmount; }
        public void setPackageAmount(long packageAmount) { this.packageAmount = packageAmount; }

        public int getSeats() { return seats; }
        public void setSeats(int seats) { this.seats = seats; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public RuleSet getRuleSet() { return ruleSet; }
        public void setRuleSet(RuleSet ruleSet) { this.ruleSet = ruleSet; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RuleSet {

        @JsonProperty("version")
        private String version;

        @JsonProperty("rules")
        private List<Rule> rules;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {

        @JsonProperty("ruleId")
        private String ruleId;

        @JsonProperty("ruleType")
        private String ruleType;

        /**
         * threshold can be a number OR an array of strings (for allowed_branches).
         * Jackson deserialises it as Object; callers should cast appropriately.
         */
        @JsonProperty("threshold")
        private Object threshold;

        @JsonProperty("weight")
        private double weight;

        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }

        public String getRuleType() { return ruleType; }
        public void setRuleType(String ruleType) { this.ruleType = ruleType; }

        public Object getThreshold() { return threshold; }
        public void setThreshold(Object threshold) { this.threshold = threshold; }

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
    }
}
