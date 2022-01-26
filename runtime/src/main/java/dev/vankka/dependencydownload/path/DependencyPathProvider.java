package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.dependency.Dependency;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * A {@link Path} provider for {@link Dependency Dependencies}.
 */
@FunctionalInterface
public interface DependencyPathProvider {

    /**
     * Gets the path that should be used for the provided {@link Dependency}.
     * @param dependency the dependency
     * @param relocated if the path should be for the relocated or unrelocated file
     * @return The absolute or relative path for the provided dependency
     */
    @NotNull
    Path getDependencyPath(@NotNull Dependency dependency, boolean relocated);

}
