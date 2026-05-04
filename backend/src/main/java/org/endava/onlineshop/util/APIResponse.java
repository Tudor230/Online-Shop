package org.endava.onlineshop.util;

public class APIResponse {
    private String message;
    private int statusCode;

    private APIResponse(Builder builder) {
        this.message = builder.message;
        this.statusCode = builder.statusCode;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static class Builder {
        private String message;
        private int statusCode;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public APIResponse build() {
            return new APIResponse(this);
        }
    }
}
