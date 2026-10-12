package com.placement.teamb.dto;

import com.placement.teamb.model.RankingCandidate;

import java.util.List;
import java.util.Map;

/**
 * Request body for the /api/v1/rankings/compute endpoint.
 */
public class RankingRequest {

    private String driveId;
    private List<RankingCandidate> candidatePool;
    private String graphVersion;
    private String algorithm;
    private String optimizationMetric;
    private Map<String, Object> constraints;

    public RankingRequest() {}

    public RankingRequest(String driveId, List<RankingCandidate> candidatePool,
                          String graphVersion, String algorithm,
                          String optimizationMetric,
                          Map<String, Object> constraints) {
        this.driveId = driveId;
        this.candidatePool = candidatePool;
        this.graphVersion = graphVersion;
        this.algorithm = algorithm;
        this.optimizationMetric = optimizationMetric;
        this.constraints = constraints;
    }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public List<RankingCandidate> getCandidatePool() { return candidatePool; }
    public void setCandidatePool(List<RankingCandidate> candidatePool) { this.candidatePool = candidatePool; }

    public String getGraphVersion() { return graphVersion; }
    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getOptimizationMetric() { return optimizationMetric; }
    public void setOptimizationMetric(String optimizationMetric) { this.optimizationMetric = optimizationMetric; }

    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
}