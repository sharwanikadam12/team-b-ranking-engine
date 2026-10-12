package com.placement.teamb.integration;

/**
 * Thrown when Team C's API is unreachable, returns an unexpected status code,
 * or returns a body that cannot be parsed.
 */
public class TeamCException extends RuntimeException {

    public TeamCException(String message) {
        super(message);
    }

    public TeamCException(String message, Throwable cause) {
        super(message, cause);
    }
}
