package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.logger.Logger;
import org.junit.jupiter.api.Test;

import java.util.*;

import static dev.vankka.dependencydownload.Helpers.*;
import static org.junit.jupiter.api.Assertions.*;

public class LoggerTest {

    @Test
    public void fullLoggerTest() {
        CountingLogger logger = new CountingLogger();
        List<Dependency> dependencies = Collections.singletonList(REAL_DEPENDENCY);

        DependencyManager dependencyManager = new DependencyManager(PATH_PROVIDER, logger);
        dependencyManager.addDependencies(dependencies);

        logger.expectNothing();

        dependencyManager.downloadAll(null, Collections.singletonList(REAL_REPOSITORY));
        logger.expectExactly("downloadStart", 1);
        // May not be downloaded always (static cache directory)
        logger.expectBetween("downloadDependency", 0, 1);
        logger.expectBetween("downloadSuccess", 0, 1);
        logger.expectExactly("downloadEnd", 1);
        logger.expectNothing();

        dependencyManager.relocateAll(null);
        logger.expectExactly("relocateStart", 1);
        logger.expectExactly("relocateDependency", dependencies);
        logger.expectExactly("relocateSuccess", dependencies);
        logger.expectExactly("relocateEnd", 1);
        logger.expectNothing();

        dependencyManager.loadAll(null, url -> {});
        logger.expectExactly("loadStart", 1);
        logger.expectExactly("loadDependency", dependencies);
        logger.expectExactly("loadSuccess", dependencies);
        logger.expectExactly("loadEnd", 1);
        logger.expectNothing();
    }

    private static class CountingLogger implements Logger {
        private final Map<String, List<Dependency>> received = new HashMap<>();

        public void expectExactly(String method, List<Dependency> dependencies) {
            List<Dependency> actual = received.remove(method);
            assertEquals(dependencies, actual, method);
        }

        public void expectExactly(String method, int amount) {
            List<Dependency> actual = received.remove(method);
            assertEquals(amount, actual.size(), method);
        }

        public void expectBetween(String method, int min, int max) {
            List<Dependency> actual = received.remove(method);
            int amount = actual != null ? actual.size() : 0;
            assertTrue(amount >= min && amount <= max, method + " >= " + min + " && <= " + max);
        }

        public void expectNothing() {
            assertEquals(0, received.size(), "Expected nothing else, still had keys: " + received.keySet());
        }

        private void receive(String method) {
            receive(method, null);
        }

        private void receive(String method, Dependency dependency) {
            received.computeIfAbsent(method, k -> new ArrayList<>()).add(dependency);
        }

        @Override
        public void downloadStart() {
            receive("downloadStart");
        }

        @Override
        public void downloadDependency(Dependency dependency) {
            receive("downloadDependency", dependency);
        }

        @Override
        public void downloadSuccess(Dependency dependency) {
            receive("downloadSuccess", dependency);
        }

        @Override
        public void downloadFailed(Dependency dependency, Throwable throwable) {
            receive("downloadFailed", dependency);
        }

        @Override
        public void downloadEnd() {
            receive("downloadEnd");
        }

        @Override
        public void relocateStart() {
            receive("relocateStart");
        }

        @Override
        public void relocateDependency(Dependency dependency) {
            receive("relocateDependency", dependency);
        }

        @Override
        public void relocateSuccess(Dependency dependency) {
            receive("relocateSuccess", dependency);
        }

        @Override
        public void relocateFailed(Dependency dependency, Throwable throwable) {
            fail("relocateFailed", throwable);
        }

        @Override
        public void relocateEnd() {
            receive("relocateEnd");
        }

        @Override
        public void loadStart() {
            receive("loadStart");
        }

        @Override
        public void loadDependency(Dependency dependency) {
            receive("loadDependency", dependency);
        }

        @Override
        public void loadSuccess(Dependency dependency) {
            receive("loadSuccess", dependency);
        }

        @Override
        public void loadFailed(Dependency dependency, Throwable throwable) {
            receive("loadFailed", dependency);
        }

        @Override
        public void loadEnd() {
            receive("loadEnd");
        }
    }
}