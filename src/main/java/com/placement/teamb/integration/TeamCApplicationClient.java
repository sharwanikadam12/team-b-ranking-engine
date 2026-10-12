package com.placement.teamb.integration;

import org.springframework.stereotype.Service;

@Service
public class TeamCApplicationClient {

    private final TeamCClient teamCClient;

    public TeamCApplicationClient(TeamCClient teamCClient) {
        this.teamCClient = teamCClient;
    }

    public String getApplications() {

        return teamCClient.getApplications();
    }
}