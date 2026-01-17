package com.microservice_employee.exception;

public class TalanaUpstreamException extends RuntimeException {
    private final int status;

    public TalanaUpstreamException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
