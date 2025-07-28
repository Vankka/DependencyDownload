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

package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.dependency.MavenDependency;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.repository.MavenRepository;
import dev.vankka.dependencydownload.repository.Repository;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Paths;

public final class Helpers {

    private Helpers() {}

    public static final DependencyPathProvider PATH_PROVIDER = DependencyPathProvider.directory(Paths.get("build", "integration-test"));
    public static final DependencyPathProvider PATH_PROVIDER_FOR_CLEANUP = DependencyPathProvider.directory(Paths.get("build", "integration-test-cleanup"));

    public static final Dependency FAKE_DEPENDENCY_1 = new MavenDependency("a", "a-a", "", "", "", "SHA-256");
    public static final Dependency FAKE_DEPENDENCY_2 = new MavenDependency("b", "b-a", "", "", "", "SHA-256");

    public static final Repository REAL_REPOSITORY = new MavenRepository("https://repo1.maven.org/maven2");
    public static final Dependency REAL_DEPENDENCY = new MavenDependency(
            "dev.vankka",
            "dependencydownload-runtime",
            "1.3.1",
            null,
            "8d0e52f1c260ff090fa8d3130ac299d1d1490fcc9ee0454dd846ad6be9e6fd7b",
            "SHA-256"
    );
    public static final Relocation REAL_RELOCATION = new Relocation("dev.vankka", "test.dev.vankka", null, null);

    public static final Repository FAKE_REPOSITORY = new MavenRepository("") {
        @Override
        public URLConnection openConnection(Dependency dependency) throws IOException {
            throw new IOException("Failed");
        }
    };
}
