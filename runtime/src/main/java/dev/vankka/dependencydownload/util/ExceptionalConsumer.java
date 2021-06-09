package dev.vankka.dependencydownload.util;

/**
 * Helper class to provide a Consumer that throws {@link Throwable}.
 *
 * @param <T> the consumable's type
 */
@FunctionalInterface
public interface ExceptionalConsumer<T> {

    void run(T t) throws Throwable;
}
