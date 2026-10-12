package com.placement.teamb.graph;

import java.util.List;
import java.util.Map;

public interface SkillGraph {

    void addStudent(String studentId, List<String> skills);

    List<String> getStudentSkills(String studentId);

    Map<String, List<String>> getGraph();

    int getStudentCount();

    int getSkillCount();
}