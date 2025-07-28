/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Vankka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
