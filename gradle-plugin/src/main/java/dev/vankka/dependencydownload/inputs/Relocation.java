package dev.vankka.dependencydownload.inputs;

import org.gradle.api.tasks.Input;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Relocation {

    private final String pattern;
    private final String shadedPattern;
    private final Set<String> includes;
    private final Set<String> excludes;

    public Relocation() {
        this(null, null, null, null);
    }

    public Relocation(String pattern, String shadedPattern, List<String> includes, List<String> excludes) {
        this.pattern = pattern(pattern);
        this.shadedPattern = pattern(shadedPattern);
        this.includes = includes != null ? includes.stream().map(this::pattern).collect(Collectors.toSet()) : null;
        this.excludes = excludes != null ? excludes.stream().map(this::pattern).collect(Collectors.toSet()) : null;
    }

    private String pattern(String pattern) {
        if (pattern == null) {
            return null;
        }

        pattern = pattern.replace('/', '.');
        if (pattern.endsWith(".")) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        return pattern;
    }

    @Input
    public String getPattern() {
        return pattern;
    }

    @Input
    public String getShadedPattern() {
        return shadedPattern;
    }

    @Input
    public Set<String> getIncludes() {
        return includes;
    }

    @Input
    public Set<String> getExcludes() {
        return excludes;
    }
}
