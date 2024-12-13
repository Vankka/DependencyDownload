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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vankka.dependencydownload.Helpers.*;
import static org.junit.jupiter.api.Assertions.*;

public class DependencyMangerTest {

    @Test
    public void duplicationTest() {
        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER);

        assertEquals(0, dependencyManager.getDependencies().size());
        dependencyManager.addDependencies(FAKE_DEPENDENCY_1);
        assertEquals(1, dependencyManager.getDependencies().size());

        // Expected, only ApplicationDependencyManager handles duplicates
        dependencyManager.addDependencies(FAKE_DEPENDENCY_1);
        assertEquals(2, dependencyManager.getDependencies().size());
    }

    @Test
    public void addDependenciesTest() {
        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER);

        assertEquals(0, dependencyManager.getAllPaths(true).size());
        assertEquals(0, dependencyManager.getDependencies().size());

        dependencyManager.addDependencies(FAKE_DEPENDENCY_1);
        assertEquals(1, dependencyManager.getDependencies().size());
        assertEquals(1, dependencyManager.getPaths(false).size());
        assertEquals(1, dependencyManager.getPaths(true).size());
        assertEquals(2, dependencyManager.getAllPaths(true).size());

        dependencyManager.addDependencies(FAKE_DEPENDENCY_2);
        assertEquals(2, dependencyManager.getDependencies().size());
        assertEquals(2, dependencyManager.getPaths(false).size());
        assertEquals(2, dependencyManager.getPaths(true).size());
        assertEquals(4, dependencyManager.getAllPaths(true).size());
    }

    @Test
    public void test() {
        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER);
        dependencyManager.addDependencies(REAL_DEPENDENCY);
        dependencyManager.addRelocations(REAL_RELOCATION);

        assertEquals(Collections.singletonList(REAL_DEPENDENCY), dependencyManager.getDependencies(), "dependencies match");
        assertEquals(Collections.singletonList(REAL_RELOCATION), dependencyManager.getRelocations(), "relocations match");

        CompletableFuture<Void> download = dependencyManager.downloadAll(null, Collections.singletonList(REAL_REPOSITORY));
        assertNotNull(download, "download future is not null");
        assertTrue(download.isDone(), "download future is done");
        assertFalse(download.isCompletedExceptionally(), "download did not fail");

        CompletableFuture<Void> relocate = dependencyManager.relocateAll(null);
        assertNotNull(relocate, "relocate future is not null");
        assertTrue(relocate.isDone(), "relocate future is done");
        assertFalse(relocate.isCompletedExceptionally(), "relocate did not fail");

        AtomicInteger calledTimes = new AtomicInteger(0);
        CompletableFuture<Void> load = dependencyManager.loadAll(null, url -> calledTimes.incrementAndGet());
        assertNotNull(load, "load future is not null");
        assertTrue(load.isDone(), "load future is done");
        assertFalse(load.isCompletedExceptionally(), "load did not fail");
        assertEquals(1, calledTimes.get(), "ClasspathAppender called only once");
        assertTrue(dependencyManager.isLoaded(), "DependencyManager.isLoaded");
    }

    @Test
    public void cleanupTest() throws IOException {
        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER_FOR_CLEANUP);
        dependencyManager.addDependencies(FAKE_DEPENDENCY_1); // only 1

        Path path1 = dependencyManager.getPathForDependency(FAKE_DEPENDENCY_1, false);
        Files.createDirectories(path1.getParent());
        Files.write(path1, new byte[0]);
        assertTrue(Files.exists(path1), "file 1 exists before cleanup");

        Path path1relocated = dependencyManager.getPathForDependency(FAKE_DEPENDENCY_1, true);
        Files.write(path1relocated, new byte[0]);
        assertTrue(Files.exists(path1relocated), "relocated file 1 exists before cleanup");

        Path path2 = dependencyManager.getPathForDependency(FAKE_DEPENDENCY_2, false);
        Files.write(path2, new byte[0]);
        assertTrue(Files.exists(path2), "file 2 exists before cleanup");

        Path path2relocated = dependencyManager.getPathForDependency(FAKE_DEPENDENCY_2, true);
        Files.write(path2relocated, new byte[0]);
        assertTrue(Files.exists(path2relocated), "relocated file 2 exists before cleanup");

        dependencyManager.cleanupCacheDirectory();
        assertTrue(Files.exists(path1), "file 1 exists after cleanup");
        assertFalse(Files.exists(path2), "file 2 no longer exists after cleanup");
        assertTrue(Files.exists(path1relocated), "relocated file 1 exists after cleanup");
        assertFalse(Files.exists(path2relocated), "relocated file 2 no longer exists after cleanup");
    }

    @Test
    public void downloadFailTest() {
        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER);
        dependencyManager.addDependencies(FAKE_DEPENDENCY_1);

        CompletableFuture<Void> future = dependencyManager.downloadAll(null, Collections.singletonList(FAKE_REPOSITORY));
        assertTrue(future.isDone(), "download future is done");
        assertTrue(future.isCompletedExceptionally(), "download future did fail");
        try {
            future.get();
        } catch (ExecutionException e) {
            assertThrows(RuntimeException.class, () -> {
                throw e.getCause();
            });
        } catch (InterruptedException e) {
            fail("Interrupted", e);
        }
    }
}
