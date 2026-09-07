package com.fcm.authzcraft.pep.runtime;

public class AuthzCraftPepException extends RuntimeException {
    public AuthzCraftPepException(String message) {
        super(message);
    }

    public AuthzCraftPepException(String message, Throwable cause) {
        super(message, cause);
    }
}