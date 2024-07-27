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

package dev.vankka.dependencydownload.logger;

import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.dependency.Dependency;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class DependencyDownloadSlf4jLogger implements Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DependencyManager.class);

    @Override
    public void downloadDependency(Dependency dependency) {
        LOGGER.info("Downloading {}", dependency.getMavenArtifact());
    }

    @Override
    public void downloadSuccess(Dependency dependency) {
        LOGGER.info("Downloaded {}", dependency.getMavenArtifact());
    }

    @Override
    public void downloadFailed(Dependency dependency, Throwable throwable) {
        LOGGER.error("Failed to download {}", dependency.getMavenArtifact());
    }

    @Override
    public void relocateStart() {
        LOGGER.info("Relocating dependencies...");
    }

    @Override
    public void relocateDependency(Dependency dependency) {
        LOGGER.debug("Relocating {}", dependency.getMavenArtifact());
    }

    @Override
    public void loadStart() {
        LOGGER.info("Loading dependencies...");
    }

    @Override
    public void loadDependency(Dependency dependency) {
        LOGGER.debug("Loading {}", dependency.getMavenArtifact());
    }

    @Override
    public void loadEnd() {
        LOGGER.info("Loaded dependencies");
    }
}
