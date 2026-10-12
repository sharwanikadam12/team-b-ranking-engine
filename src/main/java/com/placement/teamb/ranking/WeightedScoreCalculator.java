package com.placement.teamb.ranking;

import java.util.List;

import com.placement.teamb.model.DriveRequirement;
import com.placement.teamb.model.RankingCandidate;

public class WeightedScoreCalculator {

    public double calculateSkillScore(
            RankingCandidate candidate,
            List<String> requiredSkills,
            double skillWeight) {

        if (candidate == null
                || candidate.getSkills() == null
                || requiredSkills == null
                || requiredSkills.isEmpty()) {

            return 0.0;
        }

        int matchedSkills = 0;

        for (String skill : candidate.getSkills()) {

            if (requiredSkills.contains(skill)) {
                matchedSkills++;
            }
        }

        double matchPercentage =
                (double) matchedSkills / requiredSkills.size();

        return matchPercentage * skillWeight;
    }

    public double calculateAcademicScore(
            RankingCandidate candidate,
            double academicWeight) {

        if (candidate == null) {
            return 0.0;
        }

        return candidate.getCgpa() * academicWeight;
    }

    public double calculateAptitudeScore(
            RankingCandidate candidate,
            double aptitudeWeight) {

        if (candidate == null) {
            return 0.0;
        }

        return candidate.getAptitudeScore() * aptitudeWeight;
    }

    public double calculateTotalScore(
            RankingCandidate candidate,
            List<String> requiredSkills,
            DriveRequirement skillRequirement,
            double cgpaWeight,
            double aptitudeWeight) {

        double skillScore = 0.0;

        if (skillRequirement != null) {

            skillScore = calculateSkillScore(
                    candidate,
                    requiredSkills,
                    skillRequirement.getWeight()
            );
        }

        double academicScore =
                calculateAcademicScore(
                        candidate,
                        cgpaWeight
                );

        double aptitudeScore =
                calculateAptitudeScore(
                        candidate,
                        aptitudeWeight
                );

        return skillScore
                + academicScore
                + aptitudeScore;
    }
}