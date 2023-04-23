package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.common.util.HashUtil;
import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.path.CleanupPathProvider;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.path.DirectoryDependencyPathProvider;
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
import java.util.stream.Stream;

/**
 * The main class responsible for downloading, optionally relocating and loading in dependencies.
 */
@SuppressWarnings("unused") // API
public class DependencyManager {

    private final DependencyPathProvider dependencyPathProvider;

    private final List<Dependency> dependencies = new CopyOnWriteArrayList<>();
    private final Set<Relocation> relocations = new CopyOnWriteArraySet<>();
    private final AtomicInteger step = new AtomicInteger(0);

    /**
     * Creates a {@link DependencyManager}, uses the {@link DirectoryDependencyPathProvider}.
     * @param cacheDirectory the directory used for downloaded and relocated dependencies.
     * @see DirectoryDependencyPathProvider
     */
    public DependencyManager(@NotNull Path cacheDirectory) {
        this(new DirectoryDependencyPathProvider(cacheDirectory));
    }

    /**
     * Creates a {@link DependencyManager}.
     * @param dependencyPathProvider the dependencyPathProvider used for downloaded and relocated dependencies
     */
    public DependencyManager(@NotNull DependencyPathProvider dependencyPathProvider) {
        this.dependencyPathProvider = dependencyPathProvider;
    }

    /**
     * Adds a dependency to this {@link DependencyManager}.
     * @param dependency the dependency to add
     * @throws IllegalStateException if this method is executed after downloading
     * @see #addDependencies(Collection)
     */
    public void addDependency(@NotNull Dependency dependency) {
        addDependencies(Collections.singleton(dependency));
    }

    /**
     * Adds dependencies to this {@link DependencyManager}.
     * @param dependencies the dependencies to add
     * @throws IllegalStateException if this method is executed after downloading
     * @see #addDependency(Dependency)
     */
    public void addDependencies(@NotNull Collection<Dependency> dependencies) {
        if (step.get() > 0) {
            throw new IllegalStateException("Cannot add dependencies after downloading");
        }
        this.dependencies.addAll(dependencies);
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
     * @param relocation the relocation to add
     * @throws IllegalStateException if this method is executed after relocating
     * @see #addRelocations(Collection)
     */
    public void addRelocation(@NotNull Relocation relocation) {
        addRelocations(Collections.singleton(relocation));
    }

    /**
     * Adds relocations to this {@link DependencyManager}.
     * @param relocations the relocations to add
     * @throws IllegalStateException if this method is executed after relocating
     * @see #addRelocation(Relocation)
     */
    public void addRelocations(@NotNull Collection<Relocation> relocations) {
        if (step.get() > 2) {
            throw new IllegalStateException("Cannot add relocations after relocating");
        }
        this.relocations.addAll(relocations);
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
     * Gets the dependency path provider for this {@link DependencyManager}.
     * @return the instance of {@link DependencyPathProvider} or {@code null}
     * @see DependencyPathProvider
     */
    @NotNull
    public DependencyPathProvider getDependencyPathProvider() {
        return dependencyPathProvider;
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param resourceURL the url to the resource
     * @throws IOException if the resource cannot be read
     */
    public void loadFromResource(@NotNull URL resourceURL) throws IOException {
        DependencyDownloadResource resource = new DependencyDownloadResource(resourceURL);
        loadFromResource(resource);
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param fileContents the contents of the file generated by the gradle plugin as a {@link String}
     */
    public void loadFromResource(@NotNull String fileContents) {
        DependencyDownloadResource resource = new DependencyDownloadResource(fileContents);
        loadFromResource(resource);
    }

    /**
     * Loads dependencies and relocations from the resource generated by the gradle plugin.
     *
     * @param fileLines all the lines from the file generated by the gradle plugin
     */
    public void loadFromResource(@NotNull List<String> fileLines) {
        DependencyDownloadResource resource = new DependencyDownloadResource(fileLines);
        loadFromResource(resource);
    }

    /**
     * Loads dependencies and relocations from the resource provided.
     *
     * @param resource the resource
     */
    public void loadFromResource(@NotNull DependencyDownloadResource resource) {
        dependencies.addAll(resource.getDependencies());
        relocations.addAll(resource.getRelocations());
    }

    /**
     * Download all the dependencies in this {@link DependencyManager}.
     *
     * @param executor the executor that will run the downloads, or {@code null} to run it on the current thread
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
     * @param executor the executor that will run the downloads, or {@code null} to run it on the current thread
     * @param repositories an ordered list of repositories that will be tried one-by-one, in order
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
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
     * @param executor the executor that will run the relocations, or {@code null} to run it on the current thread
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
     * @param executor the executor that will run the relocations
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor, ClassLoader)
     * @see #relocateAll(Executor)
     * @see #relocate(Executor, ClassLoader)
     */
    public CompletableFuture<Void>[] relocate(@Nullable Executor executor) {
        return relocate(executor, getClass().getClassLoader());
    }

    /**
     * Relocates all the dependencies with the relocations in this {@link DependencyManager}. This step is not required.
     * If one of the relocation fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will run the relocations, or {@code null} to run it on the current thread
     * @param jarRelocatorLoader the {@link ClassLoader} to use to load {@code jar-relocator}
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
     * @throws IllegalStateException if dependencies have already been queued for relocation once
     * @see #relocateAll(Executor, ClassLoader)
     * @see #relocateAll(Executor)
     * @see #relocate(Executor)
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
     * @param executor the executor that will load the dependencies, or {@code null} to run it on the current thread
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
     * If one of the loads fails, the rest will not be tried and will not get {@link CompletableFuture}s.
     *
     * @param executor the executor that will load the dependencies, or {@code null} to run it on the current thread
     * @param classpathAppender the classpath appender
     * @return an array containing a {@link CompletableFuture} for at least one dependency but up to one for each dependency
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
     * @param relocated if the path should be for the relocated or unrelocated file of the Dependency
     * @return the path for the dependency
     */
    @NotNull
    public Path getPathForDependency(@NotNull Dependency dependency, boolean relocated) {
        return getDependencyPathProvider().getDependencyPath(dependency, relocated);
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link DependencyManager},
     * optionally also including the relocated paths if {@code includeRelocated} is set to {@code true}.
     * @param includeRelocated if relocated paths should also be included
     * @return paths to all dependencies (and optionally relocated dependencies)
     * @see #getPathForDependency(Dependency, boolean)
     */
    @NotNull
    public Set<Path> getAllPaths(boolean includeRelocated) {
        Set<Path> paths = new HashSet<>();
        for (Dependency dependency : dependencies) {
            paths.add(getPathForDependency(dependency, false));
            if (includeRelocated) {
                paths.add(getPathForDependency(dependency, true));
            }
        }
        return paths;
    }

    /**
     * Removes files that are not known dependencies of this {@link DependencyManager} from {@link CleanupPathProvider#getCleanupPath()} implementation.
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
        Path cacheDirectory = ((CleanupPathProvider) dependencyPathProvider).getCleanupPath();
        Set<Path> paths = getAllPaths(true);
        Set<Path> filesToDelete;
        try (Stream<Path> stream = Files.list(cacheDirectory)) {
            filesToDelete = stream
                    // Ignore directories
                    .filter(path -> !Files.isDirectory(path))
                    // Ignore files in this DependencyManager
                    .filter(path -> !paths.contains(path))
                    .collect(Collectors.toSet());
        }

        for (Path path : filesToDelete) {
            Files.delete(path);
        }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void>[] forEachDependency(
            Executor executor,
            ExceptionalConsumer<Dependency> runnable,
            BiFunction<Dependency, Throwable, Throwable> dependencyException
    ) {
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

    private void downloadDependency(Dependency dependency, List<Repository> repositories)
            throws IOException, NoSuchAlgorithmException {
        Path dependencyPath = getPathForDependency(dependency, false);

        if (!Files.exists(dependencyPath.getParent())) {
            Files.createDirectories(dependencyPath.getParent());
        }

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

    private void downloadFromRepository(
            Dependency dependency,
            Repository repository,
            Path dependencyPath,
            MessageDigest digest
    ) throws Throwable {
        HttpsURLConnection connection = repository.openConnection(dependency);

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

    private void relocateDependency(Dependency dependency, JarRelocatorHelper helper) {
        Path dependencyFile = getPathForDependency(dependency, false);
        Path relocatedFile = getPathForDependency(dependency, true);

        try {
            helper.run(dependencyFile, relocatedFile, relocations);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to run relocation", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to initialize relocator", e);
        }
    }

    private void loadDependency(
            Dependency dependency,
            ClasspathAppender classpathAppender,
            boolean relocated
    ) throws MalformedURLException {
        Path fileToLoad = relocated
                          ? getPathForDependency(dependency, true)
                          : getPathForDependency(dependency, false);

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

        public void run(Path from, Path to, Set<Relocation> relocations) throws ReflectiveOperationException {
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
