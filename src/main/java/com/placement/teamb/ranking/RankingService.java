package com.placement.teamb.ranking;

import com.placement.teamb.dto.RankingRequest;
import com.placement.teamb.dto.RankingResult;

public interface RankingService {

    RankingResult computeRanking(RankingRequest request);
}