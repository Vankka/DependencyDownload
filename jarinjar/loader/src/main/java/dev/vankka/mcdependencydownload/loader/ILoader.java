package dev.vankka.mcdependencydownload.loader;

import dev.vankka.mcdependencydownload.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.loader.exception.LoadingException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * A bootstrap loader, {@link #initialize()} should be called from the constructor.
 */
@SuppressWarnings("unused") // API
public interface ILoader {

    /**
     * Initializes the {@link JarInJarClassLoader} and loads in the {@link #getBootstrapClassName()}, and then calls {@link #initiateBootstrap(Class, JarInJarClassLoader)} with that class the the class loader made previously.
     * @throws LoadingException if initialization fails
     */
    @MustBeInvokedByOverriders
    @ApiStatus.NonExtendable
    default void initialize() {
        try {
            JarInJarClassLoader classLoader = new JarInJarClassLoader(getName(), getJarInJarResource(), getParentClassLoader());

            Class<?> clazz = classLoader.loadClass(getBootstrapClassName());
            initiateBootstrap(clazz, classLoader);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    classLoader.close();
                } catch (IOException e) {
                    System.err.println("Failed to shutdown JarInJarClassLoader");
                    e.printStackTrace();
                }
            }, getName() + " JarInJarClassLoader ShutdownHook"));
        } catch (Throwable t) {
            handleLoadingException(new LoadingException("Unable to load JarInJar", t));
        }
    }

    /**
     * {@link LoadingException} handler for this {@link ILoader}, by default the exception is simply thrown into the constructor through {@link #initialize()}.
     * @param exception the loading exception
     */
    default void handleLoadingException(LoadingException exception) {
        throw exception;
    }

    /**
     * The fully qualified class name for the bootstrap class, this class usually extends {@code AbstractBootstrap}.
     * @return the bootstrap class name
     */
    String getBootstrapClassName();

    /**
     * Get the constructor from the class and create a new instance, the provided {@link JarInJarClassLoader} should be used in the constructor.
     *
     * @param bootstrapClass the bootstrap class resolved from {@link #getBootstrapClassName()}
     * @param classLoader the {@link JarInJarClassLoader} that was used to load the bootstrap class and should be provided in the constructor
     * @throws NoSuchMethodException a convenience throw, will be thrown as {@link LoadingException} in {@link #initialize()}
     * @throws InvocationTargetException a convenience throw, will be thrown as {@link LoadingException} in {@link #initialize()}
     * @throws InstantiationException a convenience throw, will be thrown as {@link LoadingException} in {@link #initialize()}
     * @throws IllegalAccessException a convenience throw, will be thrown as {@link LoadingException} in {@link #initialize()}
     */
    void initiateBootstrap(Class<?> bootstrapClass, JarInJarClassLoader classLoader)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;

    /**
     * Returns the name of the plugin/mod/extension this is used for the jarinjar file name which is stored in the system's temporary file directory and shutdown hook thread name.
     * @return the name of the plugin/mod/extension
     */
    String getName();

    /**
     * The parent {@link ClassLoader} that loaded in this loader, this is used for the {@link JarInJarClassLoader}.
     * @return the parent dev.vankka.mcruntimedependencydownload.bootstrap.classloader
     */
    ClassLoader getParentClassLoader();

    /**
     * The {@link URL} to the JarInJar resource that contains the bootstrap.
     * @return the url to the JarInJar resource
     */
    URL getJarInJarResource();
}
