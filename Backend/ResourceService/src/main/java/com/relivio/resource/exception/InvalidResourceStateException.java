package com.relivio.resource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidResourceStateException extends RuntimeException {
    public InvalidResourceStateException(String message) {
        super(message);
    }
}
