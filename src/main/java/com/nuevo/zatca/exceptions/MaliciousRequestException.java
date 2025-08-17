package com.nuevo.zatca.exceptions;

public class MaliciousRequestException extends RuntimeException {
    public MaliciousRequestException(String message) {
        super(message);
    }
}
