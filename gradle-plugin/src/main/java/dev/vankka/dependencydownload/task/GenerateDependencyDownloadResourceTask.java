package dev.vankka.dependencydownload.task;

import dev.vankka.dependencydownload.DependencyDownloadGradlePlugin;
import dev.vankka.dependencydownload.relocation.Relocation;
import dev.vankka.dependencydownload.util.HashUtil;
import org.gradle.api.Action;
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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class GenerateDependencyDownloadResourceTask extends DefaultTask {

    @Input
    abstract Property<Configuration> getConfiguration();

    @OutputDirectory
    abstract RegularFileProperty getFileLocation();

    @Input
    abstract Property<String> getFile();

    @Input
    abstract Property<Boolean> getIncludeShadowJarRelocations();

    @Input
    abstract Property<String> getHashingAlgorithm();

    private List<Relocation> relocations = new ArrayList<>();

    @Inject
    public GenerateDependencyDownloadResourceTask(ObjectFactory factory) {
        getConfiguration().convention(
                getProject().getConfigurations().getByName(DependencyDownloadGradlePlugin.BASE_CONFIGURATION_NAME));
        getFileLocation().convention(
                factory.fileProperty().fileValue(getResourceDirectory(getConfiguration().get())));
        getFile().convention(getConfiguration().get().getName() + ".txt");
        getIncludeShadowJarRelocations().convention(true);
        getHashingAlgorithm().convention("SHA-256");
    }

    //
    // Relocations
    //

    public GenerateDependencyDownloadResourceTask relocate(String pattern, String destination) {
        return relocate(pattern, destination, null);
    }

    public GenerateDependencyDownloadResourceTask relocate(String pattern, String destination, Action<Relocation> configure) {
        Relocation relocation = new Relocation(pattern, destination, new ArrayList<>(), new ArrayList<>());
        addRelocator(relocation, configure);
        return this;
    }

    public GenerateDependencyDownloadResourceTask relocate(Relocation relocation) {
        addRelocator(relocation, null);
        return this;
    }

    public GenerateDependencyDownloadResourceTask relocate(Class<? extends Relocation> relocationClass)
            throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        return relocate(relocationClass, null);
    }

    public <R extends Relocation> GenerateDependencyDownloadResourceTask relocate(Class<R> relocatorClass, Action<R> configure)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        R relocator = relocatorClass.getDeclaredConstructor().newInstance();
        addRelocator(relocator, configure);
        return this;
    }

    private <R extends Relocation> void addRelocator(R relocation, Action<R> configure) {
        if (configure != null) {
            configure.execute(relocation);
        }

        relocations.add(relocation);
    }

    //
    // Utility methods
    //

    public void configuration(Configuration configuration) {
        getConfiguration().set(configuration);
        getFileLocation().convention(
                getProject().getObjects().fileProperty().fileValue(getResourceDirectory(configuration)));
        getFile().convention(configuration.getName() + ".txt");
    }

    private File getResourceDirectory(Configuration configuration) {
        JavaPluginConvention javaPluginConvention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();
        SourceSet sourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSetOutput output = sourceSet.getOutput();

        // Use existing
        String configurationName = configuration.getName();
        for (File dir : output.getDirs()) {
            if (dir.getName().equals(configurationName)) {
                return dir;
            }
        }

        // Create new
        File generatedResourcesDir = new File(getProject().getBuildDir(), "generated-resources");
        File dependencyDownloadResources = new File(generatedResourcesDir, configurationName);
        Map<String, Object> properties = new HashMap<>();
        properties.put("builtBy", configuration);
        output.dir(properties, dependencyDownloadResources);
        return dependencyDownloadResources;
    }

    //
    // Action
    //

    @TaskAction
    public void run() throws NoSuchAlgorithmException, IOException {
        // Get the resources directory for this configuration
        File resourcesDirectory = getResourceDirectory(getConfiguration().get());

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

        List<Relocation> relocations = new ArrayList<>();
        if (getIncludeShadowJarRelocations().get()) {
            shadowJar(relocations);
        }

        for (Relocation relocation : this.relocations) {
            relocations.removeIf(rel -> relocation.getPattern().equals(rel.getPattern()));
            relocations.add(relocation);
        }

        if (!relocations.isEmpty()) {
            result.add("===RELOCATIONS");
            for (Relocation relocation : relocations) {
                result.add(relocation.getPattern());
                result.add(relocation.getShadedPattern());
                result.add("[" + String.join(",", relocation.getIncludes()) + "]");
                result.add("[" + String.join(",", relocation.getExcludes()) + "]");
            }
        }

        // Save the file
        if (!resourcesDirectory.exists()) {
            Files.createDirectories(resourcesDirectory.toPath());
        }

        File dependenciesFile = new File(getFileLocation().get().getAsFile(), getFile().get());
        if (dependenciesFile.exists()) {
            Files.delete(dependenciesFile.toPath());
        } else {
            // Create parent directory if it doesn't exist
            //noinspection ResultOfMethodCallIgnored
            dependenciesFile.getParentFile().mkdirs();
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
    private void shadowJar(List<Relocation> relocations) {
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
                case "pattern":
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

        for (int index = 0; index < patterns.size(); index++) {
            relocations.add(new Relocation(
                    patterns.get(index),
                    replacements.get(index),
                    new ArrayList<>(includes.get(index)),
                    new ArrayList<>(excludes.get(index))
            ));
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
