package com.placement.teamb.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps Team C's GET /api/v1/applications response.
 *
 * Actual shape (verified 2026-10-11):
 * {
 *   "data": [ { "_id":"APP001", "studentId":"STU001", "driveId":"DRV001",
 *               "eligibility": { "result":"ELIGIBLE", ... }, ... } ],
 *   "meta": { "count": 16, ... }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamCApplicationsResponse {

    @JsonProperty("data")
    private List<ApplicationRecord> data;

    public List<ApplicationRecord> getData() { return data; }
    public void setData(List<ApplicationRecord> data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationRecord {

        @JsonProperty("_id")
        private String id;

        @JsonProperty("studentId")
        private String studentId;

        @JsonProperty("driveId")
        private String driveId;

        @JsonProperty("eligibility")
        private Eligibility eligibility;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getDriveId() { return driveId; }
        public void setDriveId(String driveId) { this.driveId = driveId; }

        public Eligibility getEligibility() { return eligibility; }
        public void setEligibility(Eligibility eligibility) { this.eligibility = eligibility; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Eligibility {

        /** "ELIGIBLE", "NOT_ELIGIBLE", or "CONDITIONAL" */
        @JsonProperty("result")
        private String result;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
