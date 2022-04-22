package dev.vankka.dependencydownload.relocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relocation that = (Relocation) o;
        return Objects.equals(pattern, that.pattern)
                && Objects.equals(shadedPattern, that.shadedPattern)
                && Objects.equals(includes, that.includes)
                && Objects.equals(excludes, that.excludes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, shadedPattern, includes, excludes);
    }
}
