package dev.vankka.mcdependencydownload.bootstrap;

import dev.vankka.mcdependencydownload.classloader.JarInJarClassLoader;

/**
 * A bootstrap that is loaded in by a {@code ILoader} from the loader module.
 */
public abstract class AbstractBootstrap {

    private final JarInJarClassLoader classLoader;

    /**
     * The constructor.
     * @param classLoader the class that loaded in this bootstrap
     */
    public AbstractBootstrap(JarInJarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the {@link JarInJarClassLoader} that loaded in this bootstrap.
     * @return the {@link JarInJarClassLoader}
     */
    @SuppressWarnings("unused") // API
    public JarInJarClassLoader getClassLoader() {
        return classLoader;
    }
}
