package com.placement.teamb.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps Team C's GET /api/v1/students response.
 *
 * Actual shape (verified 2026-10-11):
 * {
 *   "data": [
 *     {
 *       "_id": "STU001",
 *       "name": "Aarav Patil",
 *       "branch": "CSE",
 *       "skills": ["Java","Python","MongoDB","Node.js","React"],
 *       "academic": {
 *         "cgpa": 9.28, "backlogs": 0, "attendancePct": 92,
 *         "batch": 2027, "aptitudeScore": 0
 *       }
 *     }, ...
 *   ]
 * }
 *
 * Note: aptitudeScore is 0 for all students in Team C's current data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamCStudentsResponse {

    @JsonProperty("data")
    private List<StudentRecord> data;

    public List<StudentRecord> getData() { return data; }
    public void setData(List<StudentRecord> data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentRecord {

        @JsonProperty("_id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("branch")
        private String branch;

        @JsonProperty("skills")
        private List<String> skills;

        @JsonProperty("academic")
        private Academic academic;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }

        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> skills) { this.skills = skills; }

        public Academic getAcademic() { return academic; }
        public void setAcademic(Academic academic) { this.academic = academic; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Academic {

        @JsonProperty("cgpa")
        private double cgpa;

        @JsonProperty("backlogs")
        private int backlogs;

        @JsonProperty("attendancePct")
        private double attendancePct;

        @JsonProperty("batch")
        private int batch;

        @JsonProperty("aptitudeScore")
        private double aptitudeScore;

        public double getCgpa() { return cgpa; }
        public void setCgpa(double cgpa) { this.cgpa = cgpa; }

        public int getBacklogs() { return backlogs; }
        public void setBacklogs(int backlogs) { this.backlogs = backlogs; }

        public double getAttendancePct() { return attendancePct; }
        public void setAttendancePct(double attendancePct) { this.attendancePct = attendancePct; }

        public int getBatch() { return batch; }
        public void setBatch(int batch) { this.batch = batch; }

        public double getAptitudeScore() { return aptitudeScore; }
        public void setAptitudeScore(double aptitudeScore) { this.aptitudeScore = aptitudeScore; }
    }
}
