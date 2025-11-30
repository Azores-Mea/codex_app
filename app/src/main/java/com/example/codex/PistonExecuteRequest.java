package com.example.codex;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class PistonExecuteRequest {

    public String language;
    public String version;
    public List<PistonFile> files;

    @SerializedName("stdin")
    public String stdin;

    @SerializedName("args")
    public List<String> args;

    @SerializedName("compile_timeout")
    public int compileTimeout;

    @SerializedName("run_timeout")
    public int runTimeout;

    public PistonExecuteRequest(String language, String version, String code) {
        this.language = language;
        this.version = version;
        this.files = new ArrayList<>();
        this.files.add(new PistonFile(getFileName(language), code));
        this.stdin = "";
        this.args = new ArrayList<>();
        this.compileTimeout = 10000; // 10 seconds
        this.runTimeout = 3000; // 3 seconds
    }

    private String getFileName(String language) {
        switch (language.toLowerCase()) {
            case "python":
                return "main.py";
            case "java":
                return "Main.java";
            case "c++":
                return "main.cpp";
            case "c":
                return "main.c";
            case "javascript":
                return "main.js";
            default:
                return "main." + language;
        }
    }

    public static class PistonFile {
        public String name;
        public String content;

        public PistonFile(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}