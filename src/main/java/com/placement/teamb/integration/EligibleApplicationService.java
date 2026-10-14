package com.placement.teamb.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.placement.teamb.dto.EligibleCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fetches eligible student IDs (and their application IDs) for a given drive
 * from Team C's GET /api/v1/applications endpoint.
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
 *
 * The application's own {@code _id} (e.g. "APP001") is now preserved in the
 * returned {@link EligibleCandidate} objects so that it can be included in the
 * ranking write-back payload sent to Team C.
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
     * Returns the list of {@link EligibleCandidate} objects (studentId + applicationId)
     * that are ELIGIBLE for the given drive.
     *
     * @param driveId  the drive to filter by (e.g. "DRV001")
     * @return list of eligible candidates; empty if none are eligible or Team C fails
     */
    public List<EligibleCandidate> getEligibleApplications(String driveId) {

        if (driveId == null || driveId.isBlank()) {
            log.warn("getEligibleApplications called with blank driveId");
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

        List<EligibleCandidate> eligibleCandidates = new ArrayList<>();

        for (TeamCApplicationsResponse.ApplicationRecord app
                : response.getData()) {

            if (app == null) continue;

            log.debug("Application {} | student={} drive={} eligibility={}",
                    app.getId(), app.getStudentId(), app.getDriveId(),
                    app.getEligibility() != null
                            ? app.getEligibility().getResult() : "null");

            if (driveId.equalsIgnoreCase(app.getDriveId())
                    && app.getEligibility() != null
                    && "ELIGIBLE".equalsIgnoreCase(
                            app.getEligibility().getResult())) {

                eligibleCandidates.add(
                        new EligibleCandidate(driveId, app.getStudentId(), app.getId()));
                log.info("Eligible: applicationId={} studentId={} drive={}",
                        app.getId(), app.getStudentId(), driveId);
            }
        }

        log.info("Found {} eligible candidates for drive {}",
                eligibleCandidates.size(), driveId);

        return eligibleCandidates;
    }

    /**
     * Backward-compatible convenience method returning just the student IDs
     * that are ELIGIBLE for the given drive.
     *
     * @param driveId the drive to filter by (e.g. "DRV001")
     * @return list of student IDs; empty if none are eligible or Team C fails
     */
    public List<String> getEligibleStudentIds(String driveId) {
        return getEligibleApplications(driveId).stream()
                .map(EligibleCandidate::getStudentId)
                .toList();
    }
}