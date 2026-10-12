package com.placement.teamb.ranking;

import java.util.Arrays;

import com.placement.teamb.model.DriveRequirement;
import com.placement.teamb.model.RankingCandidate;

public class WeightedScoreCalculatorTest {

    public static void main(String[] args) {

        RankingCandidate candidate = new RankingCandidate(
                "TEST_STUDENT",
                Arrays.asList("Java", "Python", "SQL"),
                8.5,
                0,
                85
        );

        DriveRequirement skillRequirement =
                new DriveRequirement(
                        "RULE_001",
                        "required_skills",
                        Arrays.asList("Java", "SQL", "MongoDB"),
                        60.0
                );

        WeightedScoreCalculator calculator =
                new WeightedScoreCalculator();

        double skillScore = calculator.calculateSkillScore(
                candidate,
                Arrays.asList("Java", "SQL", "MongoDB"),
                skillRequirement.getWeight()
        );

        double academicScore = calculator.calculateAcademicScore(
                candidate,
                40.0
        );

       double totalScore = calculator.calculateTotalScore(
        candidate,
        Arrays.asList("Java", "SQL", "MongoDB"),
        skillRequirement,
        40.0,
        20.0
);

        System.out.println("Skill Score: " + skillScore);
        System.out.println("Academic Score: " + academicScore);
        System.out.println("Total Score: " + totalScore);
    }
}