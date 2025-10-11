package com.example.codex;

public class User {
    public long userId;
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String usertype;
    public String classification;

    public User() {} // Default constructor for Firebase

    public User(String firstName, String lastName, String email, String password, String usertype, String classification) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.usertype = usertype;
        this.classification = classification;
    }
}
