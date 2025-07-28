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

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static dev.vankka.dependencydownload.Helpers.*;
import static org.junit.jupiter.api.Assertions.*;

public class ApplicationDependencyManagerTest {

    public ApplicationDependencyManagerTest() {}

    @Test
    public void addDependencyTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(PATH_PROVIDER);
        assertEquals(1, manager.include(Collections.singleton(FAKE_DEPENDENCY_1)).getDependencies().size());
    }

    @Test
    public void duplicationTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(PATH_PROVIDER);
        assertEquals(1, manager.include(Collections.singleton(FAKE_DEPENDENCY_1)).getDependencies().size());
        assertEquals(0, manager.include(Collections.singleton(FAKE_DEPENDENCY_1)).getDependencies().size());
    }

    @Test
    public void multipleTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(PATH_PROVIDER);
        assertEquals(1, manager.include(Collections.singleton(FAKE_DEPENDENCY_1)).getDependencies().size());
        assertEquals(1, manager.include(Collections.singleton(FAKE_DEPENDENCY_2)).getDependencies().size());
    }
}
