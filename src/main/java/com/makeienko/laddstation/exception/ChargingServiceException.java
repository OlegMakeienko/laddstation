package com.makeienko.laddstation.exception;

public class ChargingServiceException extends RuntimeException {
    public ChargingServiceException(String message) {
        super(message);
    }

    public ChargingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
} 