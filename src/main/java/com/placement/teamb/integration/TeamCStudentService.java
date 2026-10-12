package com.placement.teamb.integration;

import org.springframework.stereotype.Service;

@Service
public class TeamCStudentService {

    private final TeamCClient teamCClient;

    public TeamCStudentService(TeamCClient teamCClient) {
        this.teamCClient = teamCClient;
    }

    public String getStudents() {

        return teamCClient.getStudents();
    }
}