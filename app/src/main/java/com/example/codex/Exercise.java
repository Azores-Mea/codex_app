package com.example.codex;

public class Exercise {
    public String title;
    public String description;
    public String code;
    public String expectedOutput;
    public String language;
    public String versionIndex;

    public Exercise() {}

    public Exercise(String title, String description, String code, String expectedOutput,
                    String language, String versionIndex) {
        this.title = title;
        this.description = description;
        this.code = code;
        this.expectedOutput = expectedOutput;
        this.language = language;
        this.versionIndex = versionIndex;
    }
}