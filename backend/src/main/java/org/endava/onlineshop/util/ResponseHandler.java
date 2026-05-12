package org.endava.onlineshop.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHandler {
    public static ResponseEntity<APIResponse> errorResponse(String message, HttpStatus statusCode){
        APIResponse apiResponse = new APIResponse(message, statusCode.value());
        return ResponseEntity.status(statusCode).body(apiResponse);
    }
}
