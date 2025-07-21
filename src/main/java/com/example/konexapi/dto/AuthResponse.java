package com.example.konexapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {
    private String token;
    private String jwt;
    private String access_token;
    private String token_type;
    private String refresh_token;
    private String expires_in; 
    private int status;
    private String message;
    private boolean success;

    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getJwt() { return jwt; }
    public void setJwt(String jwt) { this.jwt = jwt; }

    public String getAccess_token() { return access_token; }
    public void setAccess_token(String access_token) { this.access_token = access_token; }

    public String getToken_type() { return token_type; }
    public void setToken_type(String token_type) { this.token_type = token_type; }

    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

    public String getExpires_in() { return expires_in; }
    public void setExpires_in(String expires_in) { this.expires_in = expires_in; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getActualToken() {
        if (token != null && !token.trim().isEmpty()) return token;
        if (jwt != null && !jwt.trim().isEmpty()) return jwt;
        if (access_token != null && !access_token.trim().isEmpty()) return access_token;
        return null;
    }


    public LocalDateTime getExpiryDateTime() {
        if (expires_in == null || expires_in.trim().isEmpty()) {
            return LocalDateTime.now().plusHours(1);
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(expires_in, formatter);
        } catch (Exception e) {
            System.err.println("Failed to parse expires_in timestamp: " + expires_in + ". Using default 1 hour expiry.");
            return LocalDateTime.now().plusHours(1);
        }
    }
}
