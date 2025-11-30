package com.example.codex;

import java.util.ArrayList;
import java.util.List;

public class PistonRuntimesResponse extends ArrayList<PistonRuntimesResponse.Runtime> {

    public static class Runtime {
        public String language;
        public String version;
        public List<String> aliases;
        public String runtime;
    }
}
