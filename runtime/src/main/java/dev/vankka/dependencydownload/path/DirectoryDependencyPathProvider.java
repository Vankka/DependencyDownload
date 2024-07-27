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

package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.dependency.Dependency;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic dependency path provider for saving files in a single directory.
 */
public class DirectoryDependencyPathProvider implements CleanupPathProvider {

    private static final String RELOCATED_FILE_PREFIX = "relocated_";
    private final Path dependencyDirectory;

    /**
     * Creates a {@link DirectoryDependencyPathProvider}.
     * @param dependencyDirectory the directory used for downloaded and relocated dependencies.
     */
    public DirectoryDependencyPathProvider(Path dependencyDirectory) {
        this.dependencyDirectory = dependencyDirectory;
    }

    @Override
    public @NotNull Path getDependencyPath(@NotNull Dependency dependency, boolean relocated) {
        return dependencyDirectory.resolve((relocated ? RELOCATED_FILE_PREFIX : "") + dependency.getStoredFileName());
    }

    @Override
    public @NotNull Collection<Path> getPathsForAllStoredDependencies() throws IOException {
        try (Stream<Path> paths = Files.list(dependencyDirectory)) {
            return paths.collect(Collectors.toList());
        }
    }
}
