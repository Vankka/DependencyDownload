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

package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.dependency.StandardDependency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class ApplicationDependencyManagerTest {

    private final Dependency dependency1 = new StandardDependency("a", "a-a", "", "", "", "");
    private final Dependency dependency2 = new StandardDependency("b", "b-a", "", "", "", "");

    public ApplicationDependencyManagerTest() throws IOException {}

    @Test
    public void addDependencyTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
    }

    @Test
    public void duplicationTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
        Assertions.assertEquals(0, manager.include(Collections.singleton(dependency1)).getDependencies().size());
    }

    @Test
    public void multipleTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency2)).getDependencies().size());
    }
}
