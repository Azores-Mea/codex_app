package com.example.codex;

public class TracingExercise {
    public String title;
    public String description;
    public String code;
    public String expectedOutput;
    public String language;

    public TracingExercise() {
    }

    public TracingExercise(String title, String description, String code, String expectedOutput, String language) {
        this.title = title;
        this.description = description;
        this.code = code;
        this.expectedOutput = expectedOutput;
        this.language = language;
    }
}