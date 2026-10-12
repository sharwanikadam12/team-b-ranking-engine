package com.placement.teamb.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjacencyListGraph implements SkillGraph {

    private final Map<String, List<String>> graph = new HashMap<>();

    @Override
    public void addStudent(String studentId, List<String> skills) {

        if (studentId == null || skills == null) {
            return;
        }

        graph.put(studentId, new ArrayList<>(skills));
    }

    @Override
    public List<String> getStudentSkills(String studentId) {

        return graph.getOrDefault(
                studentId,
                new ArrayList<>()
        );
    }

    @Override
    public Map<String, List<String>> getGraph() {

        return graph;
    }

    @Override
    public int getStudentCount() {

        return graph.size();
    }

    @Override
    public int getSkillCount() {

        return (int) graph.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .count();
    }
}