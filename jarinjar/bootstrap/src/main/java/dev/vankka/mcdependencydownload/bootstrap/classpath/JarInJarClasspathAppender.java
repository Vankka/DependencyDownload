package dev.vankka.mcdependencydownload.bootstrap.classpath;

import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.mcdependencydownload.classloader.JarInJarClassLoader;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.nio.file.Path;

/**
 * A {@link ClasspathAppender} for {@link JarInJarClassLoader}.
 */
@SuppressWarnings("unused")
public class JarInJarClasspathAppender implements ClasspathAppender {

    private final JarInJarClassLoader classLoader;

    /**
     * Creates a new instance of this classpath appender.
     * @param classLoader the {@link JarInJarClassLoader}
     */
    public JarInJarClasspathAppender(JarInJarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void appendFileToClasspath(@NotNull Path path) throws MalformedURLException {
        classLoader.addURL(path);
    }
}
