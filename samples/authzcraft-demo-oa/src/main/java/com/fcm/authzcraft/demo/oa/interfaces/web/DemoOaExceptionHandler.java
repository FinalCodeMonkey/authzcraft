package com.fcm.authzcraft.demo.oa.interfaces.web;

import com.fcm.authzcraft.common.web.ApiResponse;
import com.fcm.authzcraft.pep.runtime.AuthzCraftPepException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DemoOaExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>failure("DEMO_OA_BAD_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) throws Exception {
        AuthzCraftPepException pepException = findPepException(exception);
        if (pepException == null) {
            throw exception;
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>failure("AUTHZCRAFT_PEP_DENIED", pepException.getMessage()));
    }

    private AuthzCraftPepException findPepException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AuthzCraftPepException) {
                return (AuthzCraftPepException) current;
            }
            current = current.getCause();
        }
        return null;
    }
}
