/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Vankka
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
