package com.example.codex;

import com.google.gson.annotations.SerializedName;

public class PistonExecuteResponse {

    public String language;
    public String version;

    @SerializedName("run")
    public RunResult run;

    @SerializedName("compile")
    public CompileResult compile;

    public static class RunResult {
        public String stdout;
        public String stderr;
        public int code;
        public String signal;
        public String output;
    }

    public static class CompileResult {
        public String stdout;
        public String stderr;
        public int code;
        public String signal;
        public String output;
    }

    public String getOutput() {
        // Check for compilation errors first
        if (compile != null && compile.code != 0) {
            if (compile.stderr != null && !compile.stderr.isEmpty()) {
                return "Compilation Error:\n" + compile.stderr;
            }
            if (compile.output != null && !compile.output.isEmpty()) {
                return "Compilation Error:\n" + compile.output;
            }
        }

        // Check runtime output
        if (run != null) {
            if (run.stdout != null && !run.stdout.isEmpty()) {
                return run.stdout;
            }
            if (run.stderr != null && !run.stderr.isEmpty()) {
                return "Runtime Error:\n" + run.stderr;
            }
            if (run.output != null && !run.output.isEmpty()) {
                return run.output;
            }
        }

        return "No output";
    }

    public boolean isSuccess() {
        return (compile == null || compile.code == 0) &&
                (run != null && run.code == 0);
    }
}