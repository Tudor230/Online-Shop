package org.endava.onlineshop.util;

import org.endava.onlineshop.exception.BadRequestException;
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
}
