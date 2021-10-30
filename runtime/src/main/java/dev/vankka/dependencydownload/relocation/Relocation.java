package dev.vankka.dependencydownload.relocation;

import java.util.Set;

public class Relocation {

    private final String pattern;
    private final String shadedPattern;
    private final Set<String> includes;
    private final Set<String> excludes;

    public Relocation(String pattern, String shadedPattern, Set<String> includes, Set<String> excludes) {
        this.pattern = pattern;
        this.shadedPattern = shadedPattern;
        this.includes = includes;
        this.excludes = excludes;
    }

    public String getPattern() {
        return pattern;
    }

    public String getShadedPattern() {
        return shadedPattern;
    }

    public Set<String> getIncludes() {
        return includes;
    }

    public Set<String> getExcludes() {
        return excludes;
    }
}
