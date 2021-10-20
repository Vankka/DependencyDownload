package dev.vankka.dependencydownload.task;

import dev.vankka.dependencydownload.DependencyDownloadGradlePlugin;
import dev.vankka.dependencydownload.util.HashUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class GenerateDependencyDownloadResourceTask extends DefaultTask {

    @Classpath
    abstract Property<Configuration> getConfiguration();

    @OutputFile
    abstract RegularFileProperty getFileLocation();

    @Input
    abstract Property<Boolean> getIncludeRelocations();

    @Input
    abstract Property<String> getHashingAlgorithm();

    public void configuration(Configuration configuration) {
        getConfiguration().set(configuration);
        getFileLocation().convention(
                getProject().getObjects().fileProperty().fileValue(new File(getResourceDirectory(), configuration.getName() + ".txt")));
    }

    @Inject
    public GenerateDependencyDownloadResourceTask(ObjectFactory factory) {
        getConfiguration().convention(
                getProject().getConfigurations().getByName(DependencyDownloadGradlePlugin.BASE_CONFIGURATION_NAME));
        getFileLocation().convention(
                factory.fileProperty().fileValue(new File(getResourceDirectory(), getConfiguration().get().getName() + ".txt")));
        getIncludeRelocations().convention(true);
        getHashingAlgorithm().convention("SHA-256");
    }

    private File getResourceDirectory() {
        JavaPluginConvention javaPluginConvention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();
        SourceSet sourceSet = sourceSets.getByName("main");
        SourceSetOutput output = sourceSet.getOutput();
        File resourcesDirectory = output.getResourcesDir();
        if (resourcesDirectory == null) {
            throw new IllegalStateException("Resources output directory could not be retrieved");
        }
        return resourcesDirectory;
    }

    @TaskAction
    public void run() throws NoSuchAlgorithmException, IOException {
        // Get a resources directory for the main source set of the JavaPlugin
        File resourcesDirectory = getResourceDirectory();

        // Generate the file contents
        StringJoiner result = new StringJoiner("\n");

        String hashingAlgorithm = getHashingAlgorithm().get();
        result.add("===ALGORITHM " + hashingAlgorithm);

        Property<Configuration> property = getConfiguration();
        Configuration configuration;
        if (property.isPresent()) {
            configuration = property.get();
        } else {
            throw new IllegalArgumentException("configuration must be provided");
        }

        List<String> dependencies = new ArrayList<>();
        for (Configuration config : getConfigurations(configuration)) {
            for (ResolvedDependency resolvedDependency : config.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
                for (String dependency : processDependency(resolvedDependency, hashingAlgorithm)) {
                    if (!dependencies.contains(dependency)) {
                        dependencies.add(dependency);
                    }
                }
            }
        }

        dependencies.forEach(result::add);

        if (getIncludeRelocations().get()) {
            shadowJar(result);
        }

        // Save the file
        if (!resourcesDirectory.exists()) {
            Files.createDirectories(resourcesDirectory.toPath());
        }

        File dependenciesFile = getFileLocation().get().getAsFile();
        if (dependenciesFile.exists()) {
            Files.delete(dependenciesFile.toPath());
        }
        Files.createFile(dependenciesFile.toPath());

        try (FileWriter writer = new FileWriter(dependenciesFile)) {
            writer.append(result.toString());
        }
    }

    private Set<Configuration> getConfigurations(Configuration configuration) {
        Set<Configuration> configurations = new HashSet<>();
        configurations.add(configuration);
        for (Configuration config : configuration.getExtendsFrom()) {
            configurations.addAll(getConfigurations(config));
        }
        return configurations;
    }

    @SuppressWarnings("unchecked")
    private void shadowJar(StringJoiner result) {
        Task shadowJar = getProject().getTasksByName("shadowJar", true)
                .stream().findAny().orElse(null);
        if (shadowJar == null) {
            return;
        }

        List<String> patterns = new ArrayList<>();
        List<String> replacements = new ArrayList<>();
        List<Collection<String>> includes = new ArrayList<>();
        List<Collection<String>> excludes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : shadowJar.getInputs().getProperties().entrySet()) {
            String[] keyParts = entry.getKey().split("\\.");
            if (keyParts.length != 3) {
                continue;
            }
            if (!keyParts[0].equals("relocators")) {
                continue;
            }

            int index;
            try {
                index = Integer.parseInt(keyParts[1].substring(1));
            } catch (NumberFormatException ignored) {
                continue;
            }

            Object value = entry.getValue();
            String key = keyParts[2];
            switch (key) {
                case "pathPattern":
                    set(patterns, index, (String) value);
                    break;
                case "shadedPattern":
                    set(replacements, index, (String) value);
                    break;
                case "includes":
                    set(includes, index, (Collection<String>) value);
                    break;
                case "excludes":
                    set(excludes, index, (Collection<String>) value);
                    break;
            }
        }

        if (patterns.isEmpty()) {
            // Don't include the relocation line if there are no relocations
            return;
        }

        result.add("===RELOCATIONS");
        for (int index = 0; index < patterns.size(); index++) {
            result.add(patterns.get(index));
            result.add(replacements.get(index));
            result.add("[" + String.join(",", includes.get(index)) + "]");
            result.add("[" + String.join(",", excludes.get(index)) + "]");
        }
    }

    private <T> void set(List<T> list, int index, T value) {
        int size = list.size();
        if (size < index) {
            for (int i = size; i < index; i++) {
                list.add(i, null);
            }
        } else if (size > index) {
            list.set(index, value);
            return;
        }
        list.add(index, value);
    }

    private List<String> processDependency(ResolvedDependency dependency, String hashingAlgorithm) throws NoSuchAlgorithmException, IOException {
        String hash = null;
        String snapshotVersion = null;
        for (ResolvedArtifact moduleArtifact : dependency.getModuleArtifacts()) {
            if (!moduleArtifact.getType().equals("jar")) {
                continue;
            }

            File file = moduleArtifact.getFile();
            hash = HashUtil.getFileHash(file, hashingAlgorithm);

            ComponentArtifactIdentifier componentArtifactIdentifier = moduleArtifact.getId();
            ComponentIdentifier componentIdentifier = componentArtifactIdentifier.getComponentIdentifier();
            if (componentIdentifier instanceof MavenUniqueSnapshotComponentIdentifier) {
                snapshotVersion = ((MavenUniqueSnapshotComponentIdentifier) componentIdentifier).getTimestampedVersion();
            }
            break;
        }

        List<String> dependencies = new ArrayList<>();
        if (hash != null) {
            String dependencyGroupName = dependency.getModuleGroup() + ":" + dependency.getModuleName();
            String version = dependency.getModuleVersion();
            String finalVersion = snapshotVersion != null ? version + ":" + snapshotVersion : version;
            if (finalVersion.endsWith("-SNAPSHOT")) {
                Logger logger = getLogger();
                logger.warn("");
                logger.warn(dependencyGroupName + " resolved to a non-versioned snapshot version: " + version);
                logger.warn("This is usually caused by the dependency being resolved from mavenLocal()");
                logger.warn("and the local repository containing the dependency with the version '" + version + "' (without a timestamp)");
            }
            dependencies.add(dependencyGroupName + ":" + finalVersion + " " + hash);
        }

        for (ResolvedDependency child : dependency.getChildren()) {
            dependencies.addAll(
                    processDependency(child, hashingAlgorithm)
            );
        }
        return dependencies;
    }
}
