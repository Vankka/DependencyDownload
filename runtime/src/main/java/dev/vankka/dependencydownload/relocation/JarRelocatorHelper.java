package dev.vankka.dependencydownload.relocation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JarRelocatorHelper {

    private final Constructor<?> relocatorConstructor;
    private final Method relocatorRunMethod;

    private final Constructor<?> relocationConstructor;

    public JarRelocatorHelper(ClassLoader classLoader) {
        try {
            Class<?> relocatorClass = classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");
            this.relocatorConstructor = relocatorClass.getConstructor(File.class, File.class, Collection.class);
            this.relocatorRunMethod = relocatorClass.getMethod("run");

            Class<?> relocationClass = classLoader.loadClass("me.lucko.jarrelocator.Relocation");
            this.relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to load jar-relocator from the provided ClassLoader", e);
        }
    }

    public void run(File from, File to, Set<Relocation> relocations) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Set<Object> mappedRelocations = new HashSet<>();

        for (Relocation relocation : relocations) {
            Object mapped = relocationConstructor.newInstance(
                    relocation.getPattern(),
                    relocation.getShadedPattern(),
                    relocation.getIncludes(),
                    relocation.getExcludes()
            );
            mappedRelocations.add(mapped);
        }

        Object relocator = relocatorConstructor.newInstance(from, to, mappedRelocations);
        relocatorRunMethod.invoke(relocator);
    }
}
