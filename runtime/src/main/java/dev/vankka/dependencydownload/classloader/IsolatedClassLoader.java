package dev.vankka.dependencydownload.classloader;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Utility {@link ClassLoader} to load classes onto a seperate classpath as the main application.
 * Extends {@link ClasspathAppender} for use with {@link dev.vankka.dependencydownload.DependencyManager}.
 */
@SuppressWarnings("unused") // API
public class IsolatedClassLoader extends URLClassLoader implements ClasspathAppender {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    public void appendFileToClasspath(@NotNull Path path) throws MalformedURLException {
        addURL(path.toUri().toURL());
    }
}
