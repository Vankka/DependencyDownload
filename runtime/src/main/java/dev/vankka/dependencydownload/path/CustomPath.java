package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.dependency.Dependency;

import java.nio.file.Path;

/**
 * An interface class to implement to use custom path system
 */
public interface CustomPath {

    /**
     * Set custom path to choice where file is put on disk system
     * @param dependency: An instance of dependency
     * @return The absolute or relative path for all dependencies
     */
    Path getCustomPath(Dependency dependency);

}
