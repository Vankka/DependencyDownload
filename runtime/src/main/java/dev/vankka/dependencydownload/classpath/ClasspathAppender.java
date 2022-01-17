package dev.vankka.dependencydownload.classpath;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.nio.file.Path;

/**
 * A helper class that appends a given {@link Path} to the classpath (for example by adding the path's url to a URLClassLoader).
 */
public interface ClasspathAppender {

    /**
     * Appends the given path to the classpath.
     *
     * @param path the path
     * @throws MalformedURLException in case the path needs to be turned into a URL, this can be thrown
     */
    void appendFileToClasspath(@NotNull Path path) throws MalformedURLException;
}
