package org.endava.onlineshop.util;

public record APIResponse(
        String message,
        int statusCode
) {
}
