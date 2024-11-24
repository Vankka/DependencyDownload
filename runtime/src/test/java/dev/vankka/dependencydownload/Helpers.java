package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.dependency.Dependency;
import dev.vankka.dependencydownload.dependency.MavenDependency;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.repository.MavenRepository;
import dev.vankka.dependencydownload.repository.Repository;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Paths;

public final class Helpers {

    private Helpers() {}

    public static final DependencyPathProvider PATH_PROVIDER = DependencyPathProvider.directory(Paths.get("build", "integration-test"));
    public static final DependencyPathProvider PATH_PROVIDER_FOR_CLEANUP = DependencyPathProvider.directory(Paths.get("build", "integration-test-cleanup"));

    public static final Dependency FAKE_DEPENDENCY_1 = new MavenDependency("a", "a-a", "", "", "", "SHA-256");
    public static final Dependency FAKE_DEPENDENCY_2 = new MavenDependency("b", "b-a", "", "", "", "SHA-256");

    public static final Repository REAL_REPOSITORY = new MavenRepository("https://repo1.maven.org/maven2");
    public static final Dependency REAL_DEPENDENCY = new MavenDependency(
            "dev.vankka",
            "dependencydownload-runtime",
            "1.3.1",
            null,
            "8d0e52f1c260ff090fa8d3130ac299d1d1490fcc9ee0454dd846ad6be9e6fd7b",
            "SHA-256"
    );
    public static final Relocation REAL_RELOCATION = new Relocation("dev.vankka", "test.dev.vankka", null, null);

    public static final Repository FAKE_REPOSITORY = new MavenRepository("") {
        @Override
        public URLConnection openConnection(Dependency dependency) throws IOException {
            throw new IOException("Failed");
        }
    };
}
