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

import java.nio.file.Path;

/**
 * Default dependency path provider, automatically used when using the {@link dev.vankka.dependencydownload.DependencyManager#DependencyManager(Path)} constructor.
 */
public class DirectoryDependencyPathProvider implements CleanupPathProvider {

    private static final String RELOCATED_FILE_PREFIX = "relocated_";
    private final Path cacheDirectory;

    /**
     * Creates a {@link DirectoryDependencyPathProvider}.
     * @param cacheDirectory the directory used for downloaded and relocated dependencies.
     */
    public DirectoryDependencyPathProvider(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public @NotNull Path getCleanupPath() {
        return cacheDirectory;
    }

    @Override
    public @NotNull Path getDependencyPath(@NotNull Dependency dependency, boolean relocated) {
        return cacheDirectory.resolve((relocated ? RELOCATED_FILE_PREFIX : "") + dependency.getStoredFileName());
    }
}
