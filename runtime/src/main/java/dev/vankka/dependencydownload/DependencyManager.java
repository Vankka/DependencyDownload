package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.common.util.HashUtil;
import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.path.CustomPath;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.repository.Repository;
import dev.vankka.dependencydownload.resource.DependencyDownloadResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * The main class responsible for downloading, optionally relocating and loading in dependencies.
 */
@SuppressWarnings("unused") // API
public class DependencyManager {

    private static final String RELOCATED_FILE_PREFIX = "relocated_";

    private final Path cacheDirectory;

    private final List<Dependency> dependencies = new CopyOnWriteArrayList<>();
    private final Set<Relocation> relocations = new CopyOnWriteArraySet<>();
    private final AtomicInteger step = new AtomicInteger(0);
    private CustomPath customPath;

    /**
     * Creates a {@link DependencyManager}.
     * @param cacheDirectory the directory used for downloaded and relocated dependencies.
     */
    public DependencyManager(@NotNull Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * The cache directory for this {@link DependencyManager}.
     * @return the cache directory
     */
    @NotNull
    public Path getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Adds a dependency to this {@link DependencyManager}.
     * @param dependency the dependency
     * @throws IllegalStateException if this method is executed after downloading
     */
    public void addDependency(@NotNull Dependency dependency) {
        if (step.get() > 0) {
            throw new IllegalStateException("Cannot add dependencies after downloading");
        }
        this.dependencies.add(dependency);
    }

    /**
     * Gets the dependencies in this {@link DependencyManager}.
     * @return a unmodifiable list of dependencies
     */
    @NotNull
    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Adds a relocation to this {@link DependencyManager}.
     * @param relocation the relocation
     * @throws IllegalStateException if this method is executed after relocating
     */
    public void addRelocation(@NotNull Relocation relocation) {
        if (step.get() > 2) {
            throw new IllegalStateException("Cannot add relocations after relocating");
        }
        this.relocations.add(relocation);
    }

    /**
     * Gets the relocations in this {@link DependencyManager}.
     * @return an unmodifiable set of relocations
     */
    @NotNull
    public Set<Relocation> getRelocations() {
        return Collections.unmodifiableSet(relocations);
    }

    /**
     * Gets CustomPath class implementation
     * @return implementation of CustomPath class
     * @see CustomPath
     */
    public CustomPath getCustomPath() {
        return customPath;
    }

    /**
     * Set CustomPath class implementation
     * @param customPath: The instance of CustomPath class
     * @see CustomPath
     */
    public void setCustomPath(CustomPath customPath) {
        this.customPath = customPath;
    }

    /**
     * Gets if CustomPath class is register
     * @return true if CustomPath class is register
     * @see CustomPath
     */
    public boolean asCustomPath() {
        return this.customPath != null;
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param resourceURL the url to the resource
     * @throws IOException if the resource cannot be read
     */
    public void loadFromResource(@NotNull URL resourceURL) throws IOException {
        DependencyDownloadResource resource = new DependencyDownloadResource(resourceURL);
        dependencies.addAll(resource.getDependencies());
        relocations.addAll(resource.getRelocations());
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param fileContents the contents of the file generated by the gradle plugin as a {@link String}
     */
    public void loadFromResource(@NotNull String fileContents) {
        DependencyDownloadResource resource = new DependencyDownloadResource(fileContents);
        dependencies.addAll(resource.getDependencies());
        relocations.addAll(resource.getRelocations());
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param fileLines all the lines from the file generated by the gradle plugin
     */
    public void loadFromResource(@NotNull List<String> fileLines) {
        DependencyDownloadResource resource = new DependencyDownloadResource(fileLines);
        dependencies.addAll(resource.getDependencies());
        relocations.addAll(resource.getRelocations());
    }

    /**
     * Download all the dependencies in this {@link DependencyManager}.
     *
     * @param executor the executor that will run the downloads
     * @param repositories an ordered list of repositories that will be tried one-by-one
     * @return a future that will complete exceptionally if a single dependency fails to download from all repositories,
     * otherwise completes when all dependencies are downloaded
     * @throws IllegalStateException if dependencies have already been queued for download once
     */
    public CompletableFuture<Void> downloadAll(@Nullable Executor executor, @NotNull List<Repository> repositories) {
        return CompletableFuture.allOf(download(executor, repositories));
    }

    /**
     * Download all the dependencies in this {@link DependencyManager}.
     *
     * @param executor the executor that will run the downloads
     * @param repositories an ordered list of repositories that will be tried one-by-one
     * @return an array containing a {@link CompletableFuture} for each dependency
     * @throws IllegalStateException if dependencies have already been queued for download once
     */
    public CompletableFuture<Void>[] download(@Nullable Executor executor, @NotNull List<Repository> repositories) {
        if (!step.compareAndSet(0, 1)) {
            throw new IllegalStateException("Download has already been executed");
        }
        return forEachDependency(executor, dependency -> downloadDependency(dependency, repositories),
                (dependency, cause) -> new RuntimeException("Failed to download dependency " + dependency.getMavenArtifact(), cause));
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * Uses the {@link ClassLoader} that loaded this class to acquire {@code jar-relocator}.
     *
     * @param executor the executor that will run the relocations
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * relocate otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     */
    public CompletableFuture<Void> relocateAll(@Nullable Executor executor) {
        return CompletableFuture.allOf(relocate(executor, getClass().getClassLoader()));
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     *
     * @param executor the executor that will run the relocations
     * @param jarRelocatorLoader the {@link ClassLoader} to use to load {@code jar-relocator},
     *                           if this is set to {@code null} the current class loader will be used
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * relocate otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     */
    public CompletableFuture<Void> relocateAll(@Nullable Executor executor, @Nullable ClassLoader jarRelocatorLoader) {
        return CompletableFuture.allOf(relocate(executor, jarRelocatorLoader));
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * Uses the {@link ClassLoader} that loaded this class to acquire {@code jar-relocator}.
     *
     * @param executor the executor that will run the relocations
     * @return an array containing a {@link CompletableFuture} for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     */
    public CompletableFuture<Void>[] relocate(@Nullable Executor executor) {
        return relocate(executor, getClass().getClassLoader());
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     *
     * @param executor the executor that will run the relocations
     * @param jarRelocatorLoader the {@link ClassLoader} to use to load {@code jar-relocator}
     * @return an array containing a {@link CompletableFuture} for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     */
    public CompletableFuture<Void>[] relocate(@Nullable Executor executor, @Nullable ClassLoader jarRelocatorLoader) {
        int currentStep = step.get();
        if (currentStep == 0) {
            throw new IllegalArgumentException("Download hasn't been executed");
        } else if (currentStep != 1) {
            throw new IllegalArgumentException("Relocate has already been executed");
        }
        step.set(2);

        JarRelocatorHelper helper = new JarRelocatorHelper(
                jarRelocatorLoader != null ? jarRelocatorLoader : getClass().getClassLoader());
        return forEachDependency(executor, dependency -> relocateDependency(dependency, helper),
                (dependency, cause) -> new RuntimeException("Failed to relocate dependency " + dependency.getMavenArtifact(), cause));
    }

    /**
     * Loads all the (potentially relocated) dependencies with provided {@link ClasspathAppender}.
     *
     * @param executor the executor that will load the dependencies
     * @param classpathAppender the classpath appender
     * @return a future that will complete exceptionally if any of the dependencies fail to
     * be appended by the provided {@link ClasspathAppender} otherwise completes when all dependencies are relocated
     * @throws IllegalStateException if dependencies have already been queued for load once
     */
    public CompletableFuture<Void> loadAll(@Nullable Executor executor, @NotNull ClasspathAppender classpathAppender) {
        return CompletableFuture.allOf(load(executor, classpathAppender));
    }

    /**
     * Loads all the (potentially relocated) dependencies with provided {@link ClasspathAppender}.
     *
     * @param executor the executor that will load the dependencies
     * @param classpathAppender the classpath appender
     * @return an array containing a {@link CompletableFuture} for each dependency
     * @throws IllegalStateException if dependencies have already been queued for load once
     */
    public CompletableFuture<Void>[] load(@Nullable Executor executor, @NotNull ClasspathAppender classpathAppender) {
        int currentStep = step.get();
        if (currentStep == 0) {
            throw new IllegalArgumentException("Download hasn't been executed");
        }
        step.set(3);

        return forEachDependency(executor, dependency -> loadDependency(dependency, classpathAppender, currentStep == 2),
                (dependency, cause) -> new RuntimeException("Failed to load dependency " + dependency.getMavenArtifact(), cause));
    }

    /**
     * Gets the {@link Path} where the given {@link Dependency} will be stored once downloaded.
     *
     * @param dependency the dependency.
     * @return the path for the dependency
     * @see #getRelocatedPathForDependency(Dependency)
     */
    @NotNull
    public Path getPathForDependency(@NotNull Dependency dependency) {
        if (asCustomPath())
            return getCustomPath().getCustomPath(dependency);
        String fileName = dependency.getStoredFileName();
        return cacheDirectory.resolve(fileName);
    }

    /**
     * Gets the {@link Path} where the given {@link Dependency} will be stored once relocated.
     *
     * @param dependency the dependency.
     * @return the path for the dependency
     */
    @NotNull
    public Path getRelocatedPathForDependency(@NotNull Dependency dependency) {
        String fileName = dependency.getStoredFileName();
        return cacheDirectory.resolve(RELOCATED_FILE_PREFIX + fileName);
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link DependencyManager},
     * optionally also including the relocated paths if {@code includeRelocated} is set to {@code true}.
     * @param includeRelocated if relocated paths should also be included
     * @return paths to all dependencies (and optionally relocated dependencies)
     * @see #getPathForDependency(Dependency)
     * @see #getRelocatedPathForDependency(Dependency)
     */
    @NotNull
    public Set<Path> getAllPaths(boolean includeRelocated) {
        Set<Path> paths = new HashSet<>();
        for (Dependency dependency : dependencies) {
            paths.add(getPathForDependency(dependency));
            if (includeRelocated) {
                paths.add(getRelocatedPathForDependency(dependency));
            }
        }
        return paths;
    }

    /**
     * Removes files that are not known dependencies of this {@link DependencyManager} from the {@link #getCacheDirectory()}.
     * <b>
     * This only accounts for dependencies that are included in this {@link DependencyManager} instance!
     * </b>
     *
     * @throws IOException if listing files in the cache directory or deleting files in it fails
     * @see #getAllPaths(boolean)
     */
    public void cleanupCacheDirectory() throws IOException {
        Set<Path> paths = getAllPaths(true);
        Set<Path> filesToDelete = Files.list(cacheDirectory)
                // Ignore directories
                .filter(path -> !Files.isDirectory(path))
                // Ignore files in this DependencyManager
                .filter(path -> !paths.contains(path))
                .collect(Collectors.toSet());

        for (Path path : filesToDelete) {
            Files.delete(path);
        }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void>[] forEachDependency(Executor executor, ExceptionalConsumer<Dependency> runnable,
                                                        BiFunction<Dependency, Throwable, Throwable> dependencyException) {
        int size = dependencies.size();
        CompletableFuture<Void>[] futures = new CompletableFuture[size];

        for (int index = 0; index < size; index++) {
            Dependency dependency = dependencies.get(index);

            CompletableFuture<Void> future = new CompletableFuture<>();
            Runnable run = () -> {
                try {
                    runnable.run(dependency);
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(
                            dependencyException.apply(dependency, t));
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

    private void downloadDependency(Dependency dependency, List<Repository> repositories) throws IOException, NoSuchAlgorithmException {
        if (!cacheDirectory.toFile().exists()) {
            Files.createDirectories(cacheDirectory);
        }

        Path dependencyPath = getPathForDependency(dependency);

        if (!Files.exists(dependencyPath.getParent()))
            Files.createDirectories(dependencyPath.getParent());

        if (Files.exists(dependencyPath)) {
            String fileHash = HashUtil.getFileHash(dependencyPath.toFile(), dependency.getHashingAlgorithm());
            if (fileHash.equals(dependency.getHash())) {
                // This dependency is already downloaded & the hash matches
                return;
            } else {
                Files.delete(dependencyPath);
            }
        }
        Files.createFile(dependencyPath);

        RuntimeException failure = new RuntimeException("All provided repositories failed to download dependency");
        for (Repository repository : repositories) {
            try {
                MessageDigest digest = MessageDigest.getInstance(dependency.getHashingAlgorithm());
                downloadFromRepository(dependency, repository, dependencyPath, digest);

                String hash = HashUtil.getHash(digest);
                String dependencyHash = dependency.getHash();
                if (!hash.equals(dependencyHash)) {
                    throw new RuntimeException("Failed to verify file hash: " + hash + " should've been: " + dependencyHash);
                }

                // Success
                return;
            } catch (Throwable e) {
                Files.deleteIfExists(dependencyPath);
                failure.addSuppressed(e);
            }
        }
        throw failure;
    }

    private void downloadFromRepository(Dependency dependency, Repository repository, Path dependencyPath, MessageDigest digest) throws Throwable {
        HttpsURLConnection connection = repository.openConnection(dependency);

        byte[] buffer = new byte[4096];
        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dependencyPath.toFile()))) {
                int total;
                while ((total = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, total);
                    digest.update(buffer, 0, total);
                }
            }
        }
    }

    private void relocateDependency(Dependency dependency, JarRelocatorHelper helper) {
        Path dependencyFile = getPathForDependency(dependency);
        Path relocatedFile = getRelocatedPathForDependency(dependency);

        try {
            helper.run(dependencyFile, relocatedFile, relocations);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to run relocation", e.getCause());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize relocator", e);
        }
    }

    private void loadDependency(Dependency dependency, ClasspathAppender classpathAppender, boolean relocated) throws MalformedURLException {
        Path fileToLoad = relocated
                          ? getRelocatedPathForDependency(dependency)
                          : getPathForDependency(dependency);

        classpathAppender.appendFileToClasspath(fileToLoad);
    }

    /**
     * Helper class to provide a Consumer that throws {@link Throwable}.
     *
     * @param <T> the consumable's type
     */
    @FunctionalInterface
    private interface ExceptionalConsumer<T> {

        void run(T t) throws Throwable;
    }

    private static class JarRelocatorHelper {

        private final Constructor<?> relocatorConstructor;
        private final Method relocatorRunMethod;

        private final Constructor<?> relocationConstructor;

        public JarRelocatorHelper(ClassLoader classLoader) {
            try {
                Class<?> relocatorClass = classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");
                this.relocatorConstructor = relocatorClass.getConstructor(File.class, File.class, Collection.class);
                this.relocatorRunMethod = relocatorClass.getMethod("run");

                Class<?> relocationClass = classLoader.loadClass("me.lucko.jarrelocator.Relocation");
                this.relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException("Failed to load jar-relocator from the provided ClassLoader", e);
            }
        }

        public void run(Path from, Path to, Set<Relocation> relocations) throws InvocationTargetException, InstantiationException, IllegalAccessException {
            Set<Object> mappedRelocations = new HashSet<>();

            for (Relocation relocation : relocations) {
                Object mapped = relocationConstructor.newInstance(
                        relocation.getPattern(),
                        relocation.getShadedPattern(),
                        relocation.getIncludes(),
                        relocation.getExcludes()
                );
                mappedRelocations.add(mapped);
            }

            Object relocator = relocatorConstructor.newInstance(from.toFile(), to.toFile(), mappedRelocations);
            relocatorRunMethod.invoke(relocator);
        }
    }
}
