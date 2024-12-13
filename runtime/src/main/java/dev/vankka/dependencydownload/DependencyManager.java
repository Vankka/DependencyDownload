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

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.common.util.HashUtil;
import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.logger.Logger;
import dev.vankka.dependencydownload.path.CleanupPathProvider;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.repository.Repository;
import dev.vankka.dependencydownload.resource.DependencyDownloadResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The main class responsible for downloading, optionally relocating and loading in dependencies.
 */
@SuppressWarnings("unused") // API
public class DependencyManager {

    private final DependencyPathProvider dependencyPathProvider;
    private final Logger logger;

    private final List<Dependency> dependencies = new CopyOnWriteArrayList<>();
    private final List<Relocation> relocations = new CopyOnWriteArrayList<>();

    /**
     * 0 initial, 1 download, 2 relocate, 3 load.
     */
    private final AtomicInteger step = new AtomicInteger(0);

    /**
     * Creates a {@link DependencyManager}.
     * @param dependencyPathProvider the {@link DependencyPathProvider} used for deciding where to store downloaded and relocated dependencies
     */
    public DependencyManager(@NotNull DependencyPathProvider dependencyPathProvider) {
        this(dependencyPathProvider, Logger.NOOP);
    }

    /**
     * Creates a {@link DependencyManager}.
     * @param dependencyPathProvider the {@link DependencyPathProvider} used for deciding where to store downloaded and relocated dependencies
     * @param logger the logger to use
     */
    public DependencyManager(@NotNull DependencyPathProvider dependencyPathProvider, @NotNull Logger logger) {
        this.dependencyPathProvider = dependencyPathProvider;
        this.logger = logger;
    }

    /**
     * Adds dependencies to this {@link DependencyManager}.
     * @param dependencies the dependencies to add
     * @throws IllegalStateException if this method is executed after downloading
     * @see #addDependencies(Collection)
     */
    public DependencyManager addDependencies(@NotNull Dependency... dependencies) {
        return addDependencies(Arrays.asList(dependencies));
    }

    /**
     * Adds dependencies to this {@link DependencyManager}.
     * @param dependencies the dependencies to add
     * @throws IllegalStateException if this method is executed after downloading
     * @see #addDependencies(Dependency...)
     */
    public DependencyManager addDependencies(@NotNull Collection<Dependency> dependencies) {
        if (step.get() > 0) {
            throw new IllegalStateException("Cannot add dependencies after downloading");
        }
        this.dependencies.addAll(dependencies);
        return this;
    }

    /**
     * Gets the dependencies in this {@link DependencyManager}.
     * @return an unmodifiable list of dependencies
     */
    @NotNull
    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Adds relocations to this {@link DependencyManager}.
     * @param relocations the relocations to add
     * @throws IllegalStateException if this method is executed after relocating
     * @see #addRelocations(Collection)
     */
    public DependencyManager addRelocations(@NotNull Relocation... relocations) {
        return addRelocations(Arrays.asList(relocations));
    }

    /**
     * Adds relocations to this {@link DependencyManager}.
     * @param relocations the relocations to add
     * @throws IllegalStateException if this method is executed after relocating
     * @see #addRelocations(Relocation...)
     */
    public DependencyManager addRelocations(@NotNull Collection<Relocation> relocations) {
        if (step.get() > 2) {
            throw new IllegalStateException("Cannot add relocations after relocating");
        }
        this.relocations.addAll(relocations);
        return this;
    }

    /**
     * Gets the relocations in this {@link DependencyManager}.
     * @return an unmodifiable set of relocations
     */
    @NotNull
    public List<Relocation> getRelocations() {
        return Collections.unmodifiableList(relocations);
    }

    /**
     * Gets the dependency path provider for this {@link DependencyManager}.
     * @return the instance of {@link DependencyPathProvider} or {@code null}
     * @see DependencyPathProvider
     */
    @NotNull
    public DependencyPathProvider getDependencyPathProvider() {
        return dependencyPathProvider;
    }

    /**
     * Are this {@link DependencyManager}s dependencies already loaded.
     * @return {@code true} if {@link #load(Executor, ClasspathAppender)} has already been loaded
     */
    public boolean isLoaded() {
        return step.get() == 3;
    }

    /**
     * Loads dependencies and relocations from the resource provided.
     *
     * @param resource the resource
     */
    public DependencyManager loadResource(@NotNull DependencyDownloadResource resource) {
        dependencies.addAll(resource.getDependencies());
        relocations.addAll(resource.getRelocations());
        return this;
    }

    /**
     * Download all the dependencies in this {@link DependencyManager}.
     *
     * @param executor the executor that will run the download for every dependency, or {@code null} to run sequentially on the current thread
     * @param repositories an ordered list of repositories that will be tried one-by-one in order
     * @return a future that will complete exceptionally if a single dependency fails to download from all repositories,
     * otherwise completes when all dependencies are downloaded
     * @throws IllegalStateException if dependencies have already been queued for download once
     */
    public CompletableFuture<Void> downloadAll(@Nullable Executor executor, @NotNull List<Repository> repositories) {
        return CompletableFuture.allOf(download(executor, repositories));
    }

    /**
     * Download all the dependencies in this {@link DependencyManager}.
     * If one of the downloads fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will run the download for every dependency, or {@code null} to run sequentially on the current thread
     * @param repositories an ordered list of repositories that will be tried one-by-one, in order
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for download once
     */
    public CompletableFuture<Void>[] download(@Nullable Executor executor, @NotNull List<Repository> repositories) {
        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("No repositories provided");
        }
        // If step is 0 (initial) change to 1
        if (!step.compareAndSet(0, 1)) {
            throw new IllegalStateException("Download has already been executed");
        }

        logger.downloadStart();
        try {
            return forEachDependency(
                    executor,
                    dependency -> downloadDependency(dependency, repositories, () -> logger.downloadDependency(dependency)),
                    (dependency, cause) -> new RuntimeException("Failed to download dependency " + dependency.getGAV(), cause),
                    logger::downloadSuccess,
                    logger::downloadFailed
            );
        } finally {
            logger.downloadEnd();
        }
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * Uses the {@link ClassLoader} that loaded this class to acquire {@code jar-relocator}.
     *
     * @param executor the executor that will run the relocation for every dependency, or {@code null} to run sequentially on the current thread
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * relocate otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor, ClassLoader)
     * @see #relocate(Executor)
     * @see #relocate(Executor, ClassLoader)
     */
    public CompletableFuture<Void> relocateAll(@Nullable Executor executor) {
        return CompletableFuture.allOf(relocate(executor, getClass().getClassLoader()));
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     *
     * @param executor the executor that will run the relocation for every dependency, or {@code null} to run sequentially on the current thread
     * @param jarRelocatorLoader the {@link ClassLoader} to use to load {@code jar-relocator},
     *                           if this is set to {@code null} the current class loader will be used
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * relocate otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor)
     * @see #relocate(Executor)
     * @see #relocate(Executor, ClassLoader)
     */
    public CompletableFuture<Void> relocateAll(@Nullable Executor executor, @Nullable ClassLoader jarRelocatorLoader) {
        return CompletableFuture.allOf(relocate(executor, jarRelocatorLoader));
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * Uses the {@link ClassLoader} that loaded this class to acquire {@code jar-relocator}.
     * If one of the relocation fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will run the relocation for every dependency, or {@code null} to run sequentially on the current thread
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor, ClassLoader)
     * @see #relocateAll(Executor)
     * @see #relocate(Executor, ClassLoader)
     */
    public CompletableFuture<Void>[] relocate(@Nullable Executor executor) {
        return relocate(executor, null);
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * If one of the relocation fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will run the relocation for every dependency, or {@code null} to run sequentially on the current thread
     * @param jarRelocatorLoader the {@link ClassLoader} to use to load {@code jar-relocator}
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor, ClassLoader)
     * @see #relocateAll(Executor)
     * @see #relocate(Executor)
     */
    public CompletableFuture<Void>[] relocate(@Nullable Executor executor, @Nullable ClassLoader jarRelocatorLoader) {
        // If step is 1 (load) change to 2, otherwise don't alter
        int currentStep = step.getAndUpdate(current -> current == 1 ? 2 : current);
        if (currentStep == 0) {
            throw new IllegalStateException("Download hasn't been executed");
        } else if (currentStep == 2) {
            throw new IllegalStateException("Already relocated");
        } else if (currentStep == 3) {
            throw new IllegalStateException("Cannot relocate after loading");
        }

        JarRelocatorHelper helper = new JarRelocatorHelper(
                jarRelocatorLoader != null ? jarRelocatorLoader : getClass().getClassLoader(),
                relocations
        );

        try {
            logger.relocateStart();
            return forEachDependency(
                    executor,
                    dependency -> {
                        logger.relocateDependency(dependency);
                        return relocateDependency(dependency, helper);
                    },
                    (dependency, cause) -> new RuntimeException("Failed to relocate dependency " + dependency.getGAV(), cause),
                    logger::relocateSuccess,
                    logger::relocateFailed
            );
        } finally {
            logger.relocateEnd();
        }
    }

    /**
     * Loads all the (potentially relocated) dependencies with provided {@link ClasspathAppender}.
     *
     * @param executor the executor that will run the provided classpath appender for every dependency, or {@code null} to run sequentially on the current thread
     * @param classpathAppender the classpath appender, that will handle loading the file
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * be appended by the provided {@link ClasspathAppender} otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for load once
     */
    public CompletableFuture<Void> loadAll(@Nullable Executor executor, @NotNull ClasspathAppender classpathAppender) {
        return CompletableFuture.allOf(load(executor, classpathAppender));
    }

    /**
     * Loads all the (potentially relocated) dependencies with provided {@link ClasspathAppender}.
     * If one of the loads fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will run the provided classpath appender for every dependency, or {@code null} to run sequentially on the current thread
     * @param classpathAppender the classpath appender
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for load once
     */
    public CompletableFuture<Void>[] load(@Nullable Executor executor, @NotNull ClasspathAppender classpathAppender) {
        // If step is 1 (download), 2 (relocate) change to 3 (load), otherwise keep current
        int currentStep = step.getAndUpdate(current -> current == 0 || current == 3 ? current : 3);
        if (currentStep == 0) {
            throw new IllegalArgumentException("Download hasn't been executed");
        } else if (currentStep == 3) {
            throw new IllegalArgumentException("Already loaded");
        }

        try {
            logger.loadStart();

            return forEachDependency(
                    executor,
                    dependency -> {
                        logger.loadDependency(dependency);
                        return loadDependency(dependency, classpathAppender, currentStep == 2);
                    },
                    (dependency, cause) -> new RuntimeException("Failed to load dependency " + dependency.getGAV(), cause),
                    logger::loadSuccess,
                    logger::loadFailed
            );
        } finally {
            logger.loadEnd();
        }
    }

    /**
     * Gets the {@link Path} where the given {@link Dependency} will be stored once downloaded or relocated.
     *
     * @param dependency the dependency.
     * @param relocated if the path should be for the relocated or unrelocated (downloaded) file of the Dependency
     * @return the path for the dependency
     */
    @NotNull
    public Path getPathForDependency(@NotNull Dependency dependency, boolean relocated) {
        return getDependencyPathProvider().getDependencyPath(dependency, relocated);
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link DependencyManager}. Including ones that do not exist.
     * @param relocated the paths for all relocated files, otherwise all unrelocated (downloaded) files
     * @return paths to all dependencies, original or relocated
     * @see #getPathForDependency(Dependency, boolean)
     */
    @NotNull
    public Set<Path> getPaths(boolean relocated) {
        Set<Path> paths = new HashSet<>();
        for (Dependency dependency : dependencies) {
            paths.add(getPathForDependency(dependency, relocated));
        }
        return paths;
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link DependencyManager},
     * optionally also including the relocated paths if {@code includeRelocated} is set to {@code true}.
     * Including ones that do not exist.
     *
     * @param includeRelocated if relocated paths should also be included
     * @return paths to all dependencies (and optionally relocated dependencies)
     * @see #getPathForDependency(Dependency, boolean)
     */
    @NotNull
    public Set<Path> getAllPaths(boolean includeRelocated) {
        Set<Path> paths = new HashSet<>(getPaths(false));
        if (includeRelocated) {
            paths.addAll(getPaths(true));
        }
        return paths;
    }

    /**
     * Removes files that are not known dependencies of this {@link DependencyManager} from {@link CleanupPathProvider#getPathsForAllStoredDependencies()} implementation.
     * <b>
     * This only accounts for dependencies that are included in this {@link DependencyManager} instance!
     * </b>
     *
     * @throws IOException if listing files in the cache directory or deleting files in it fails
     * @throws IllegalStateException if this DependencyManager's dependencyPathProvider isn't an instance of {@link CleanupPathProvider}
     * @see #getAllPaths(boolean)
     * @see CleanupPathProvider
     */
    public void cleanupCacheDirectory() throws IOException, IllegalStateException {
        if (!(dependencyPathProvider instanceof CleanupPathProvider)) {
            throw new IllegalStateException("Cache directory cleanup is only available when dependencyPathProvider is a instance of CleanupPathProvider");
        }

        Collection<Path> existingPaths = ((CleanupPathProvider) dependencyPathProvider).getPathsForAllStoredDependencies();
        Set<Path> currentPaths = getAllPaths(true);
        for (Path existingPath : existingPaths) {
            if (Files.isDirectory(existingPath)) {
                continue;
            }

            if (!currentPaths.contains(existingPath)) {
                Files.delete(existingPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void>[] forEachDependency(
            Executor executor,
            Step<Dependency> runnable,
            BiFunction<Dependency, Throwable, Throwable> dependencyException,
            Consumer<Dependency> successLog,
            BiConsumer<Dependency, Throwable> failLog
    ) {
        int size = dependencies.size();
        CompletableFuture<Void>[] futures = new CompletableFuture[size];

        for (int index = 0; index < size; index++) {
            Dependency dependency = dependencies.get(index);

            CompletableFuture<Void> future = new CompletableFuture<>();
            Runnable run = () -> {
                try {
                    boolean stepPerformed = runnable.run(dependency);
                    future.complete(null);

                    if (stepPerformed) {
                        successLog.accept(dependency);
                    }
                } catch (Throwable t) {
                    future.completeExceptionally(dependencyException.apply(dependency, t));
                    failLog.accept(dependency, t);
                }
            };

            if (executor != null) {
                executor.execute(run);
            } else {
                run.run();
            }

            futures[index] = future;
            if (future.isCompletedExceptionally()) {
                // don't need to bother with the rest if one fails
                break;
            }
        }

        return futures;
    }

    private boolean downloadDependency(Dependency dependency, List<Repository> repositories, Runnable beginDownloadCallback)
            throws IOException, NoSuchAlgorithmException {
        Path dependencyPath = getPathForDependency(dependency, false);

        if (!Files.exists(dependencyPath.getParent())) {
            Files.createDirectories(dependencyPath.getParent());
        }

        MessageDigest digest = MessageDigest.getInstance(dependency.getHashingAlgorithm());
        if (Files.exists(dependencyPath)) {
            String fileHash = HashUtil.getFileHash(dependencyPath, digest);
            if (fileHash.equals(dependency.getHash())) {
                // This dependency is already downloaded & the hash matches -> skip download
                return false;
            } else {
                // Hash does not match, delete file
                Files.delete(dependencyPath);
            }
        }
        beginDownloadCallback.run();
        Files.createFile(dependencyPath);

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        boolean anyFailures = false;
        for (Repository repository : repositories) {
            try {
                digest.reset();
                downloadFromRepository(dependency, repository, dependencyPath, digest);

                String hash = HashUtil.getHash(digest);
                String dependencyHash = dependency.getHash();
                if (!hash.equals(dependencyHash)) {
                    throw new SecurityException("Failed to verify file hash: " + hash + " should've been: " + dependencyHash);
                }

                // Success
                return true;
            } catch (Exception e) {
                Files.deleteIfExists(dependencyPath);
                failure.addSuppressed(e);
                anyFailures = true;
            }
        }
        if (!anyFailures) {
            throw new IllegalStateException("Nothing failed yet nothing passed");
        }
        throw failure;
    }

    private void downloadFromRepository(
            Dependency dependency,
            Repository repository,
            Path dependencyPath,
            MessageDigest digest
    ) throws IOException {
        URLConnection connection = repository.openConnection(dependency);

        byte[] buffer = new byte[repository.getBufferSize()];
        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(dependencyPath))) {
                int total;
                while ((total = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, total);
                    digest.update(buffer, 0, total);
                }
            }
        }
    }

    private boolean relocateDependency(Dependency dependency, JarRelocatorHelper helper) {
        Path dependencyFile = getPathForDependency(dependency, false);
        Path relocatedFile = getPathForDependency(dependency, true);

        try {
            helper.run(dependencyFile, relocatedFile);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to run relocation", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize relocator", e);
        }
        return true;
    }

    private boolean loadDependency(
            Dependency dependency,
            ClasspathAppender classpathAppender,
            boolean relocated
    ) throws MalformedURLException {
        Path fileToLoad = relocated
                          ? getPathForDependency(dependency, true)
                          : getPathForDependency(dependency, false);

        classpathAppender.appendFileToClasspath(fileToLoad);
        return true;
    }

    @FunctionalInterface
    private interface Step<T> {

        /**
         * @return {@code true} if the step was performed, {@code false} if skipped
         */
        boolean run(T t) throws Throwable;
    }

    private static class JarRelocatorHelper {

        private final Constructor<?> relocatorConstructor;
        private final Method relocatorRunMethod;

        private final List<Object> mappedRelocations;

        public JarRelocatorHelper(ClassLoader classLoader, List<Relocation> relocations) {
            try {
                Class<?> relocatorClass = classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");
                this.relocatorConstructor = relocatorClass.getConstructor(File.class, File.class, Collection.class);
                this.relocatorRunMethod = relocatorClass.getMethod("run");

                Class<?> relocationClass = classLoader.loadClass("me.lucko.jarrelocator.Relocation");
                Constructor<?> relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);

                this.mappedRelocations = new ArrayList<>();
                for (Relocation relocation : relocations) {
                    Object mapped = relocationConstructor.newInstance(
                            relocation.getPattern(),
                            relocation.getShadedPattern(),
                            relocation.getIncludes(),
                            relocation.getExcludes()
                    );
                    mappedRelocations.add(mapped);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to load jar-relocator from the provided ClassLoader", e);
            }
        }

        public void run(Path from, Path to) throws ReflectiveOperationException {
            Object relocator = relocatorConstructor.newInstance(from.toFile(), to.toFile(), mappedRelocations);
            relocatorRunMethod.invoke(relocator);
        }
    }
}
