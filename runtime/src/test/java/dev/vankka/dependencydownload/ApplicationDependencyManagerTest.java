package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.dependency.StandardDependency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class ApplicationDependencyManagerTest {

    private final Dependency dependency1 = new StandardDependency("a", "a-a", "", "", "", "");
    private final Dependency dependency2 = new StandardDependency("b", "b-a", "", "", "", "");

    public ApplicationDependencyManagerTest() throws IOException {}

    @Test
    public void addDependencyTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
    }

    @Test
    public void duplicationTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
        Assertions.assertEquals(0, manager.include(Collections.singleton(dependency1)).getDependencies().size());
    }

    @Test
    public void multipleTest() {
        ApplicationDependencyManager manager = new ApplicationDependencyManager(Paths.get("."));
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency1)).getDependencies().size());
        Assertions.assertEquals(1, manager.include(Collections.singleton(dependency2)).getDependencies().size());
    }
}
