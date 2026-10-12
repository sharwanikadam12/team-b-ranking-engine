package com.placement.teamb.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjacencyMatrixGraph implements SkillGraph {

    private final List<String> students = new ArrayList<>();
    private final List<String> skills = new ArrayList<>();

    private final Map<String, Integer> studentIndex = new HashMap<>();
    private final Map<String, Integer> skillIndex = new HashMap<>();

    private boolean[][] matrix = new boolean[0][0];

    @Override
    public void addStudent(String studentId, List<String> studentSkills) {

        if (studentId == null || studentSkills == null) {
            return;
        }

        // Add student if not already present
        if (!studentIndex.containsKey(studentId)) {
            studentIndex.put(studentId, students.size());
            students.add(studentId);
        }

        // Add any new skills
        for (String skill : studentSkills) {

            if (skill != null && !skillIndex.containsKey(skill)) {
                skillIndex.put(skill, skills.size());
                skills.add(skill);
            }
        }

        /*
         * Save the old matrix before resizing.
         * This prevents previously stored student-skill
         * connections from being lost.
         */
        boolean[][] oldMatrix = matrix;

        int oldStudentCount = oldMatrix.length;
        int oldSkillCount =
                oldStudentCount > 0 ? oldMatrix[0].length : 0;

        // Create new matrix
        matrix = new boolean[students.size()][skills.size()];

        // Copy old data into new matrix
        for (int i = 0; i < oldStudentCount; i++) {

            for (int j = 0; j < oldSkillCount; j++) {

                matrix[i][j] = oldMatrix[i][j];
            }
        }

        // Add the new student's skill connections
        int studentPosition = studentIndex.get(studentId);

        for (String skill : studentSkills) {

            if (skill != null) {

                Integer skillPosition = skillIndex.get(skill);

                if (skillPosition != null) {
                    matrix[studentPosition][skillPosition] = true;
                }
            }
        }
    }

    @Override
    public List<String> getStudentSkills(String studentId) {

        List<String> result = new ArrayList<>();

        Integer studentPosition = studentIndex.get(studentId);

        if (studentPosition == null) {
            return result;
        }

        for (int j = 0; j < skills.size(); j++) {

            if (matrix[studentPosition][j]) {
                result.add(skills.get(j));
            }
        }

        return result;
    }

    @Override
    public Map<String, List<String>> getGraph() {

        Map<String, List<String>> graph = new HashMap<>();

        for (String student : students) {

            graph.put(
                    student,
                    getStudentSkills(student)
            );
        }

        return graph;
    }

    @Override
    public int getStudentCount() {

        return students.size();
    }

    @Override
    public int getSkillCount() {

        return skills.size();
    }
}