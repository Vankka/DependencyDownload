package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.DependencyManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * An interface extend of {@link DependencyPathProvider}
 * This interface must be implemented to support {@link DependencyManager#cleanupCacheDirectory()}
 */
public interface CleanupPathProvider extends DependencyPathProvider {

    /**
     * Gets the path that should be used for removing unused files using {@link dev.vankka.dependencydownload.DependencyManager#cleanupCacheDirectory}.
     * @return The absolute or relative path use for cleanup directory (should be the directory where the dependencies are stored)
     */
    @NotNull
    Path getCleanupPath();

}
