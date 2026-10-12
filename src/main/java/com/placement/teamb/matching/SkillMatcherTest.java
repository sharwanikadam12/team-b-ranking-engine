package com.placement.teamb.matching;

import java.util.Arrays;

import com.placement.teamb.model.RankingCandidate;

public class SkillMatcherTest {

    public static void main(String[] args) {

        RankingCandidate candidate = new RankingCandidate(
                "TEST_STUDENT",
                Arrays.asList("Java", "Python", "SQL"),
                8.5,
                0,
                85
        );

        SkillMatcher matcher = new SkillMatcher();

        System.out.println("Student Skills: "
                + candidate.getSkills());

        System.out.println("Required Skills: "
                + Arrays.asList("Java", "SQL", "MongoDB"));

        System.out.println("Matched Skills: "
                + matcher.findMatchedSkills(
                        candidate,
                        Arrays.asList("Java", "SQL", "MongoDB")
                ));

        System.out.println("Match Count: "
                + matcher.getMatchCount(
                        candidate,
                        Arrays.asList("Java", "SQL", "MongoDB")
                ));
    }
}