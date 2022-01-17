package dev.vankka.dependencydownload.relocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * A relocation.
 */
public class Relocation {

    private final String pattern;
    private final String shadedPattern;
    private final Set<String> includes;
    private final Set<String> excludes;

    public Relocation(@NotNull String pattern, @NotNull String shadedPattern, @Nullable Set<String> includes, @Nullable Set<String> excludes) {
        this.pattern = pattern;
        this.shadedPattern = shadedPattern;
        this.includes = includes != null ? includes : Collections.emptySet();
        this.excludes = excludes != null ? excludes : Collections.emptySet();
    }

    @NotNull
    public String getPattern() {
        return pattern;
    }

    @NotNull
    public String getShadedPattern() {
        return shadedPattern;
    }

    @NotNull
    public Set<String> getIncludes() {
        return includes;
    }

    @NotNull
    public Set<String> getExcludes() {
        return excludes;
    }
}
