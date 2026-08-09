package com.relivio.reliefrequest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestStateException extends RuntimeException {
    public InvalidRequestStateException(String message) {
        super(message);
    }
}
