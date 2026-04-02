package org.endava.onlineshop.util;

public class APIResponse {
    String message;
    int statusCode;

    private APIResponse(Builder builder){
        builder.build();
    }

    public static class Builder{
        private String message;
        private int statusCode;

        public Builder message(String Message){
            this.message=message;
            return this;
        }

        public Builder statusCode(int statusCode){
            this.statusCode=statusCode;
            return this;
        }

        public APIResponse build(){
            return new APIResponse(this);
        }
    }
}
