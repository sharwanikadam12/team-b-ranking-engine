package com.placement.teamb.matching;

import java.util.ArrayList;
import java.util.List;

import com.placement.teamb.model.RankingCandidate;

public class SkillMatcher {

    public List<String> findMatchedSkills(
            RankingCandidate candidate,
            List<String> requiredSkills) {

        List<String> matchedSkills = new ArrayList<>();

        if (candidate == null
                || candidate.getSkills() == null
                || requiredSkills == null) {
            return matchedSkills;
        }

        for (String skill : candidate.getSkills()) {

            if (requiredSkills.contains(skill)) {
                matchedSkills.add(skill);
            }
        }

        return matchedSkills;
    }

    public int getMatchCount(
            RankingCandidate candidate,
            List<String> requiredSkills) {

        return findMatchedSkills(candidate, requiredSkills).size();
    }
}