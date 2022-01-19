package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.dependency.Dependency;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * A {@link Path} provider for {@link Dependency Dependencies}.
 */
@FunctionalInterface
@NotNull
public interface DependencyPathProvider {

    /**
     * Gets the path that should be used for the provided {@link Dependency}.
     * @param dependency the dependency
     * @param relocated: true if it is a relocated dependency
     * @return The absolute or relative path for the provided dependency
     */
    @NotNull
    Path getDependencyPathProvider(@NotNull Dependency dependency, boolean relocated);

}
