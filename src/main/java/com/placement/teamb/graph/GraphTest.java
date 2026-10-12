package com.placement.teamb.graph;

import java.util.Arrays;

public class GraphTest {

    public static void main(String[] args) {

        // Test 1: Adjacency List
        System.out.println("===== ADJACENCY LIST TEST =====");

        AdjacencyListGraph listGraph = new AdjacencyListGraph();

        listGraph.addStudent(
                "S001",
                Arrays.asList("Java", "Python", "SQL")
        );

        listGraph.addStudent(
                "S002",
                Arrays.asList("Java", "HTML")
        );

        System.out.println("S001 skills: "
                + listGraph.getStudentSkills("S001"));

        System.out.println("S002 skills: "
                + listGraph.getStudentSkills("S002"));

        System.out.println("Total students: "
                + listGraph.getStudentCount());

        System.out.println("Total unique skills: "
                + listGraph.getSkillCount());


        // Test 2: Adjacency Matrix
        System.out.println();
        System.out.println("===== ADJACENCY MATRIX TEST =====");

        AdjacencyMatrixGraph matrixGraph =
                new AdjacencyMatrixGraph();

        matrixGraph.addStudent(
                "S001",
                Arrays.asList("Java", "Python", "SQL")
        );

        matrixGraph.addStudent(
                "S002",
                Arrays.asList("Java", "HTML")
        );

        System.out.println("S001 skills: "
                + matrixGraph.getStudentSkills("S001"));

        System.out.println("S002 skills: "
                + matrixGraph.getStudentSkills("S002"));

        System.out.println("Total students: "
                + matrixGraph.getStudentCount());

        System.out.println("Total unique skills: "
                + matrixGraph.getSkillCount());
    }
}