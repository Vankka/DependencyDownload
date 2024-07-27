package dev.vankka.dependencydownload.logger;

import dev.vankka.dependencydownload.dependency.Dependency;

/**
 * Logger for DependencyDownload, override methods you want to log.
 */
@SuppressWarnings("unused")
public interface Logger {

    default void downloadStart() {}
    default void downloadEnd() {}

    default void downloadDependency(Dependency dependency) {}
    default void downloadSuccess(Dependency dependency) {}
    default void downloadFailed(Dependency dependency, Throwable throwable) {}

    default void relocateStart() {}
    default void relocateEnd() {}

    default void relocateDependency(Dependency dependency) {}
    default void relocateSuccess(Dependency dependency) {}
    default void relocateFailed(Dependency dependency, Throwable throwable) {}

    default void loadStart() {}
    default void loadEnd() {}

    default void loadDependency(Dependency dependency) {}
    default void loadSuccess(Dependency dependency) {}
    default void loadFailed(Dependency dependency, Throwable throwable) {}

    class NOOP implements Logger {
        public static NOOP INSTANCE = new NOOP();

        private NOOP() {}
    }
}
