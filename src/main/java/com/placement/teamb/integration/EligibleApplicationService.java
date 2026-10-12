package com.placement.teamb.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches eligible student IDs for a given drive from Team C's
 * GET /api/v1/applications endpoint.
 *
 * Team C's response shape (verified 2026-10-11):
 * {
 *   "data": [
 *     { "_id":"APP001", "studentId":"STU001", "driveId":"DRV001",
 *       "eligibility": { "result":"ELIGIBLE" }, ... }
 *   ]
 * }
 *
 * Only records where driveId matches the requested drive AND
 * eligibility.result == "ELIGIBLE" are included.
 */
@Service
public class EligibleApplicationService {

    private static final Logger log =
            LoggerFactory.getLogger(EligibleApplicationService.class);

    private final TeamCClient teamCClient;
    private final ObjectMapper objectMapper;

    public EligibleApplicationService(TeamCClient teamCClient) {
        this.teamCClient = teamCClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns the list of student IDs that are ELIGIBLE for the given drive.
     *
     * @param driveId  the drive to filter by (e.g. "DRV001")
     * @return list of student IDs; empty if none are eligible or Team C fails
     */
    public List<String> getEligibleStudentIds(String driveId) {

        if (driveId == null || driveId.isBlank()) {
            log.warn("getEligibleStudentIds called with blank driveId");
            return Collections.emptyList();
        }

        String rawJson;
        try {
            rawJson = teamCClient.getApplications();
        } catch (TeamCException e) {
            log.error("Failed to fetch applications from Team C: {}",
                    e.getMessage());
            throw e;
        }

        TeamCApplicationsResponse response;
        try {
            response = objectMapper.readValue(
                    rawJson, TeamCApplicationsResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Team C applications response: {}",
                    e.getMessage());
            throw new TeamCException(
                    "Team C applications response could not be parsed", e);
        }

        if (response.getData() == null) {
            log.warn("Team C returned null data array for applications");
            return Collections.emptyList();
        }

        List<String> eligibleStudentIds = new ArrayList<>();

        for (TeamCApplicationsResponse.ApplicationRecord app
                : response.getData()) {

            if (app == null) continue;

            log.debug("Application {} | student={} drive={} eligibility={}",
                    app.getId(), app.getStudentId(), app.getDriveId(),
                    app.getEligibility() != null
                            ? app.getEligibility().getResult() : "null");

            if (driveId.equals(app.getDriveId())
                    && app.getEligibility() != null
                    && "ELIGIBLE".equalsIgnoreCase(
                            app.getEligibility().getResult())) {

                eligibleStudentIds.add(app.getStudentId());
                log.info("Eligible student {} for drive {}",
                        app.getStudentId(), driveId);
            }
        }

        log.info("Found {} eligible students for drive {}",
                eligibleStudentIds.size(), driveId);

        return eligibleStudentIds;
    }
}