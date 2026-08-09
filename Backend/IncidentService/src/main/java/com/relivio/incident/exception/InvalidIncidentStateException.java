package com.relivio.incident.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidIncidentStateException extends RuntimeException {
    public InvalidIncidentStateException(String message) {
        super(message);
    }
}
