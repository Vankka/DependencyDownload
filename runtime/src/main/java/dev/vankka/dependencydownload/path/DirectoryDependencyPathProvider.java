package dev.vankka.dependencydownload.path;

import dev.vankka.dependencydownload.dependency.Dependency;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Default dependency path provider, automatically use when call of constructor {@link dev.vankka.dependencydownload.DependencyManager#DependencyManager(Path)}
 */
public class DirectoryDependencyPathProvider implements CleanupPathProvider {

    private static final String RELOCATED_FILE_PREFIX = "relocated_";
    private final Path cacheDirectory;

    /**
     * Creates a {@link DirectoryDependencyPathProvider}.
     * @param cacheDirectory the directory used for downloaded and relocated dependencies.
     */
    public DirectoryDependencyPathProvider(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public @NotNull Path getCleanupPath() {
        return cacheDirectory;
    }

    @Override
    public @NotNull Path getDependencyPath(@NotNull Dependency dependency, boolean relocated) {
        return cacheDirectory.resolve((relocated ? RELOCATED_FILE_PREFIX : "") + dependency.getStoredFileName());
    }
}
