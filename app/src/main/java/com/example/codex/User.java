package com.example.codex;

public class User {
    public long userId;
    public String firstName, lastName, email, password, usertype, classification;
    public String educationalBackground;
    public String fieldOfStudy;
    public String learningMode;

    public User() {}

    public User(String firstName, String lastName, String email, String password, String usertype, String classification, String educationalBackground, String fieldOfStudy) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fieldOfStudy = fieldOfStudy;
        this.educationalBackground = educationalBackground;
        this.email = email;
        this.password = password;
        this.usertype = usertype;
        this.classification = classification;
    }
}
