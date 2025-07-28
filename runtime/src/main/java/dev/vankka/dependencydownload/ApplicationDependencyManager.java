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
import dev.vankka.dependencydownload.logger.Logger;
import dev.vankka.dependencydownload.path.CleanupPathProvider;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.resource.DependencyDownloadResource;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * An application level dependency manager to prevent loading in the same dependency multiple times.
 */
@SuppressWarnings("unused") // API
public class ApplicationDependencyManager {

    private final DependencyManager dependencyManager;

    /**
     * Creates a {@link ApplicationDependencyManager}.
     * @param dependencyPathProvider the dependencyPathProvider used for downloaded and relocated dependencies
     */
    public ApplicationDependencyManager(@NotNull DependencyPathProvider dependencyPathProvider) {
        this(dependencyPathProvider, Logger.NOOP);
    }
    /**
     * Creates a {@link ApplicationDependencyManager}.
     * @param dependencyPathProvider the dependencyPathProvider used for downloaded and relocated dependencies
     * @param logger the logger to use
     */
    public ApplicationDependencyManager(@NotNull DependencyPathProvider dependencyPathProvider, Logger logger) {
        this.dependencyManager = new DependencyManager(dependencyPathProvider, logger);
    }

    /**
     * Gets the dependency path provider for this {@link ApplicationDependencyManager}.
     * @return the instance of {@link DependencyPathProvider}
     */
    @NotNull
    public DependencyPathProvider getDependencyPathProvider() {
        return dependencyManager.getDependencyPathProvider();
    }

    /**
     * Gets the logger being used by this {@link ApplicationDependencyManager}.
     * @return the instance of {@link Logger} being used
     */
    @NotNull
    public Logger getLogger() {
        return dependencyManager.getLogger();
    }

    /**
     * Adds the provided relocations to this {@link ApplicationDependencyManager},
     * they will be used in all {@link DependencyManager}s created by this manager after being added.
     * @param relocations the relocations to add
     * @see #addRelocations(Collection)
     */
    @NotNull
    public ApplicationDependencyManager addRelocations(@NotNull Relocation... relocations) {
        return addRelocations(Arrays.asList(relocations));
    }

    /**
     * Adds the provided relocations to this {@link ApplicationDependencyManager},
     * they will be used in all {@link DependencyManager}s created by this manager after being added.
     * @param relocations the relocations to add
     * @see #addRelocations(Relocation...)
     */
    @NotNull
    public ApplicationDependencyManager addRelocations(@NotNull Collection<Relocation> relocations) {
        synchronized (dependencyManager) {
            dependencyManager.addRelocations(relocations);
        }
        return this;
    }

    /**
     * Includes the dependencies and relocations from the {@link DependencyDownloadResource} provided as an argument.
     * <p>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param resource the resource to get dependencies and relocations from
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    @NotNull
    public DependencyManager includeResource(@NotNull DependencyDownloadResource resource) {
        addRelocations(resource.getRelocations());
        return include(resource.getDependencies());
    }

    /**
     * Includes the provided dependencies.
     * <p>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param dependencies the dependencies to include
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    @NotNull
    public DependencyManager include(@NotNull Dependency... dependencies) {
        return include(Arrays.asList(dependencies));
    }

    /**
     * Includes the provided dependencies.
     * <p>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param dependencies the dependencies to include
     * @return a new {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    @NotNull
    public DependencyManager include(@NotNull Collection<Dependency> dependencies) {
        dependencies = addMissingDependencies(dependencies);

        DependencyManager dependencyManager = new DependencyManager(getDependencyPathProvider(), getLogger());
        dependencyManager.addDependencies(dependencies);
        synchronized (this.dependencyManager) {
            dependencyManager.addRelocations(this.dependencyManager.getRelocations());
        }
        return dependencyManager;
    }

    /**
     * Includes the dependencies and relocations from the provided {@link DependencyManager},
     * the {@link DependencyPathProvider} and {@link Logger} will be preserved from the provided {@link DependencyManager}.
     * <p>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager (including ones from the provided {@link DependencyManager}).
     *
     * @param manager the manager to get dependencies and relocations from
     * @return a new {@link DependencyManager} to load in the dependencies, or if the provided manager has already been loaded, it will be returned instead
     */
    @NotNull
    public DependencyManager include(@NotNull DependencyManager manager) {
        addRelocations(manager.getRelocations());
        List<Dependency> dependencies = addMissingDependencies(manager.getDependencies());
        if (manager.isLoaded()) {
            return manager;
        }

        DependencyManager dependencyManager = new DependencyManager(manager.getDependencyPathProvider(), manager.getLogger());
        dependencyManager.addDependencies(dependencies);
        synchronized (this.dependencyManager) {
            dependencyManager.addRelocations(this.dependencyManager.getRelocations());
        }
        return dependencyManager;
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
        synchronized (dependencyManager) {
            return dependencyManager.getPathForDependency(dependency, relocated);
        }
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link ApplicationDependencyManager},
     * optionally also including the relocated paths if {@code includeRelocated} is set to {@code true}.
     * @param relocated relocated paths, otherwise unrelocated paths
     * @return paths to all dependencies, original or relocated
     * @see #getPathForDependency(Dependency, boolean)
     */
    @NotNull
    public Set<Path> getPaths(boolean relocated) {
        synchronized (dependencyManager) {
            return dependencyManager.getPaths(relocated);
        }
    }

    /**
     * Gets {@link Path}s to all {@link Dependency Dependencies} in this {@link ApplicationDependencyManager},
     * optionally also including the relocated paths if {@code includeRelocated} is set to {@code true}.
     * @param includeRelocated if relocated paths should also be included
     * @return paths to all dependencies (and optionally relocated dependencies)
     * @see #getPathForDependency(Dependency, boolean)
     */
    @NotNull
    public Set<Path> getAllPaths(boolean includeRelocated) {
        synchronized (dependencyManager) {
            return dependencyManager.getAllPaths(includeRelocated);
        }
    }

    /**
     * Removes files that are not known dependencies of this {@link DependencyManager} from {@link CleanupPathProvider#getPathsForAllStoredDependencies()} implementation.
     * <b>
     * This only accounts for dependencies that are included in this {@link DependencyManager} instance!
     * </b>
     *
     * @throws IOException if listing files in the cache directory or deleting files in it fails
     * @throws IllegalStateException if this DependencyManager's dependencyPathProvider isn't an instance of {@link CleanupPathProvider}
     * @see CleanupPathProvider
     */
    public void cleanupCacheDirectory() throws IOException, IllegalStateException {
        synchronized (dependencyManager) {
            dependencyManager.cleanupCacheDirectory();
        }
    }

    private List<Dependency> addMissingDependencies(Collection<Dependency> old) {
        List<Dependency> missingDependencies = new ArrayList<>(old.size());
        for (Dependency dependency : old) {
            String group = dependency.getGroupId();
            String artifact = dependency.getArtifactId();

            synchronized (dependencyManager) {
                // Check that there is no dependency with the same group + artifact id
                boolean noMatch = dependencyManager.getDependencies().stream()
                        .noneMatch(dep -> dep.getGroupId().equals(group)
                                && dep.getArtifactId().equals(artifact));

                if (noMatch) {
                    missingDependencies.add(dependency);
                    dependencyManager.addDependencies(dependency);
                }
            }
        }
        return missingDependencies;
    }

}
