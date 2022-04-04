package dev.vankka.dependencydownload.jarinjar.loader;

import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.dependencydownload.jarinjar.loader.exception.LoadingException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.URL;

/**
 * A bootstrap loader, {@link #initialize()} should be called from the constructor.
 */
@SuppressWarnings("unused") // API
public interface ILoader {

    /**
     * Initializes the {@link JarInJarClassLoader} and loads in the {@link #getBootstrapClassName()}, and then calls {@link #initiateBootstrap(Class, JarInJarClassLoader)} with that class the class loader made previously.
     * Calls {@link #handleLoadingException(LoadingException)} if initialization fails, by default this throws the exception
     *
     * @return the {@link JarInJarClassLoader} used to load the bootstrap, this should be closed when not needed anymore
     *
     * @see #createClassLoader()
     * @see #initiateBootstrap(Class, JarInJarClassLoader)
     */
    @MustBeInvokedByOverriders
    @ApiStatus.NonExtendable
    @UnknownNullability("Not null unless handleLoadingException is modified")
    default JarInJarClassLoader initialize() {
        try {
            JarInJarClassLoader classLoader = createClassLoader();
            Class<?> clazz = classLoader.loadClass(getBootstrapClassName());
            initiateBootstrap(clazz, classLoader);

            return classLoader;
        } catch (Throwable t) {
            handleLoadingException(new LoadingException("Unable to load JarInJar", t));
            return null;
        }
    }

    /**
     * {@link LoadingException} handler for this {@link ILoader}, by default the exception is simply thrown into the constructor through {@link #initialize()}.
     * @param exception the loading exception
     */
    default void handleLoadingException(@NotNull LoadingException exception) {
        throw exception;
    }

    /**
     * Creates the class loader for this {@link ILoader}.
     * @return the new classloader
     * @throws IOException if creating a file in the temporary file directory fails
     */
    @NotNull
    default JarInJarClassLoader createClassLoader() throws IOException {
        return new JarInJarClassLoader(getName(), getJarInJarResource(), getParentClassLoader());
    }

    /**
     * The parent {@link ClassLoader} that loaded in this loader, this is used in {@link #createClassLoader()} by default.
     * @return the parent {@link ClassLoader}
     */
    @NotNull
    default ClassLoader getParentClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * Get the constructor from the class and create a new instance, the provided {@link JarInJarClassLoader} should be used in the constructor.
     *
     * @param bootstrapClass the bootstrap class resolved from {@link #getBootstrapClassName()}
     * @param classLoader the {@link JarInJarClassLoader} that was used to load the bootstrap class and should be provided in the constructor
     * @throws ReflectiveOperationException a convenience throw, will be passed as a {@link LoadingException} to {@link #handleLoadingException(LoadingException)}
     */
    void initiateBootstrap(@NotNull Class<?> bootstrapClass, @NotNull JarInJarClassLoader classLoader) throws ReflectiveOperationException;

    /**
     * The fully qualified class name for the bootstrap class, this class usually extends {@code AbstractBootstrap}.
     * @return the bootstrap class name
     */
    @NotNull
    String getBootstrapClassName();

    /**
     * Returns the name of this loader this is used for the jarinjar file name which is stored in the system's temporary file directory.
     * @return the name of this loader
     */
    @NotNull
    String getName();

    /**
     * The {@link URL} to the JarInJar resource that contains the bootstrap.
     * @return the url to the JarInJar resource
     */
    @NotNull
    URL getJarInJarResource();
}
