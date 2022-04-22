package dev.vankka.dependencydownload;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.path.DirectoryDependencyPathProvider;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.resource.DependencyDownloadResource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An application level dependency manager to prevent loading in the same dependency multiple times.
 */
@SuppressWarnings("unused") // API
public class ApplicationDependencyManager {

    private final Set<Dependency> dependencies = new CopyOnWriteArraySet<>();
    private final Set<Relocation> relocations = new CopyOnWriteArraySet<>();

    private final DependencyPathProvider dependencyPathProvider;

    /**
     * Creates a {@link ApplicationDependencyManager}, uses the {@link DirectoryDependencyPathProvider}.
     * @param cacheDirectory the directory used for downloaded and relocated dependencies.
     * @see DirectoryDependencyPathProvider
     */
    public ApplicationDependencyManager(@NotNull Path cacheDirectory) {
        this(new DirectoryDependencyPathProvider(cacheDirectory));
    }

    /**
     * Creates a {@link ApplicationDependencyManager}.
     * @param dependencyPathProvider the dependencyPathProvider used for downloaded and relocated dependencies
     */
    public ApplicationDependencyManager(@NotNull DependencyPathProvider dependencyPathProvider) {
        this.dependencyPathProvider = dependencyPathProvider;
    }

    /**
     * Adds the provided relocations to this {@link ApplicationDependencyManager},
     * they will be used in all {@link DependencyManager}s created by this manager after being added.
     * @param relocations the relocations to add
     * @see #addRelocation(Relocation)
     */
    public void addRelocations(@NotNull Collection<Relocation> relocations) {
        this.relocations.addAll(relocations);
    }

    /**
     * Adds the provided relocation to this {@link ApplicationDependencyManager},
     * it will be used in all {@link DependencyManager}s created by this manager after being added.
     * @param relocation the relocation
     * @see #addRelocations(Collection)
     */
    public void addRelocation(@NotNull Relocation relocation) {
        this.relocations.add(relocation);
    }

    /**
     * Includes the dependencies and relocations from the dependency resource generated
     * by the gradle plugin, located by the provided {@link URL}.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param resourceURL the url to the resource generated by the gradle plugin
     * @return the {@link DependencyManager} to load in the dependencies
     * @throws IOException if reading the file from the {@link URL} fails
     */
    @CheckReturnValue
    public DependencyManager includeResource(@NotNull URL resourceURL) throws IOException {
        DependencyDownloadResource resource = new DependencyDownloadResource(resourceURL);
        return includeResource(resource);
    }

    /**
     * Includes the dependencies and relocations from the dependency resource generated
     * by the gradle plugin, provided as the {@link String} content of the resource.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param resourceContent the full contents of the resource generated by the gradle plugin
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    public DependencyManager includeResource(@NotNull String resourceContent) {
        DependencyDownloadResource resource = new DependencyDownloadResource(resourceContent);
        return includeResource(resource);
    }

    /**
     * Includes the dependencies and relocations from the dependency resource generated
     * by the gradle plugin, provided as a list of lines from the resource.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param resourceLines all the lines of the resource generated by the gradle plugin
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    public DependencyManager includeResource(@NotNull List<String> resourceLines) {
        DependencyDownloadResource resource = new DependencyDownloadResource(resourceLines);
        return includeResource(resource);
    }

    /**
     * Includes the dependencies and relocations from the {@link DependencyDownloadResource} provided as an argument.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param resource the resource to get dependencies and relocations from
     * @return the {@link DependencyManager} to load in the dependencies
     */
    public DependencyManager includeResource(@NotNull DependencyDownloadResource resource) {
        return include(resource.getDependencies(), resource.getRelocations());
    }

    /**
     * Includes the provided dependencies and relocations.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param dependencies the dependencies to include
     * @param relocations the relocations to include
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    public DependencyManager include(@NotNull List<Dependency> dependencies, @NotNull List<Relocation> relocations) {
        addRelocations(relocations);
        return include(dependencies);
    }

    /**
     * Includes the provided dependencies.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param dependencies the dependencies to include
     * @return the {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    public DependencyManager include(@NotNull Collection<Dependency> dependencies) {
        dependencies = dependencies(dependencies);

        DependencyManager manager = new DependencyManager(dependencyPathProvider);
        manager.addDependencies(dependencies);
        manager.addRelocations(this.relocations);
        return manager;
    }

    /**
     * Includes the dependencies and relocations from the provided {@link DependencyManager}, the {@link DependencyPathProvider} will be preserved.
     * <br/>
     * The returned {@link DependencyManager} will only include dependencies that have not been downloaded yet,
     * and will include all the relocations from this manager.
     *
     * @param manager the manager to get dependencies and relocations from
     * @return a new {@link DependencyManager} to load in the dependencies
     */
    @CheckReturnValue
    public DependencyManager include(@NotNull DependencyManager manager) {
        addRelocations(manager.getRelocations());
        List<Dependency> dependencies = dependencies(manager.getDependencies());

        DependencyManager dependencyManager = new DependencyManager(manager.getDependencyPathProvider());
        dependencyManager.addDependencies(dependencies);
        dependencyManager.addRelocations(this.relocations);
        return dependencyManager;
    }

    private List<Dependency> dependencies(Collection<Dependency> old) {
        List<Dependency> newDependencies = new ArrayList<>(old.size());
        for (Dependency dependency : dependencies) {
            String group = dependency.getGroupId();
            String artifact = dependency.getArtifactId();

            // Check that there is no dependency with the same group + artifact id
            boolean noMatch = this.dependencies.stream()
                    .noneMatch(dep -> dep.getGroupId().equals(group)
                            && dep.getArtifactId().equals(artifact));

            if (noMatch && this.dependencies.add(dependency)) {
                newDependencies.add(dependency);
            }
        }
        return newDependencies;
    }
}
