package com.example.codex;

public class JDoodleRequest {
    String script, language, versionIndex, clientId, clientSecret;

    public JDoodleRequest(String script, String language, String versionIndex,
                          String clientId, String clientSecret) {
        this.script = script;
        this.language = language;
        this.versionIndex = versionIndex;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}

