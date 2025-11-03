package com.example.codex;

public class User {
    public long userId;
    public String firstName, lastName, email, password, usertype, classification;
    public String learningMode;

    public User() {}

    public User(String firstName, String lastName, String email, String password, String usertype, String classification) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.usertype = usertype;
        this.classification = classification;
    }
}
