package com.example.konexapi.dto;

public class AuthRequest {
    private String login;
    private String password;
    private String user_type;

    public AuthRequest(String login, String password, String user_type) {
        this.login = login;
        this.password = password;
        this.user_type = user_type;
    }


    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUser_type() { return user_type; }
    public void setUser_type(String user_type) { this.user_type = user_type; }
}