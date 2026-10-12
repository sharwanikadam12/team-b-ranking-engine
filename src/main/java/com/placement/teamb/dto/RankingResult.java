package com.placement.teamb.dto;

import com.placement.teamb.model.CandidateScore;

import java.util.List;

/**
 * Response body containing the ordered ranking result.
 */
public class RankingResult {

    private String rankingId;
    private List<CandidateScore> orderedCandidates;
    private double totalScore;
    private String tieBreakRule;
    private String algorithm;
    private String graphVersion;
    private int exploredCandidateCount;

    public RankingResult() {}

    public RankingResult(String rankingId, List<CandidateScore> orderedCandidates,
                         double totalScore, String tieBreakRule, String algorithm,
                         String graphVersion, int exploredCandidateCount) {
        this.rankingId = rankingId;
        this.orderedCandidates = orderedCandidates;
        this.totalScore = totalScore;
        this.tieBreakRule = tieBreakRule;
        this.algorithm = algorithm;
        this.graphVersion = graphVersion;
        this.exploredCandidateCount = exploredCandidateCount;
    }

    public String getRankingId() { return rankingId; }
    public void setRankingId(String rankingId) { this.rankingId = rankingId; }

    public List<CandidateScore> getOrderedCandidates() { return orderedCandidates; }
    public void setOrderedCandidates(List<CandidateScore> orderedCandidates) { this.orderedCandidates = orderedCandidates; }

    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }

    public String getTieBreakRule() { return tieBreakRule; }
    public void setTieBreakRule(String tieBreakRule) { this.tieBreakRule = tieBreakRule; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getGraphVersion() { return graphVersion; }
    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }

    public int getExploredCandidateCount() { return exploredCandidateCount; }
    public void setExploredCandidateCount(int exploredCandidateCount) { this.exploredCandidateCount = exploredCandidateCount; }
}