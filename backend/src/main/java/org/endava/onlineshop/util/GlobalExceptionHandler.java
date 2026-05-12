package org.endava.onlineshop.util;

import org.endava.onlineshop.exception.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<APIResponse> handleBadRequestException(BadRequestException e){
        return ResponseHandler.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                ? e.getMostSpecificCause().getMessage()
                : "Data integrity violation";
        return ResponseHandler.errorResponse(message, HttpStatus.CONFLICT);
    }
}
