package dev.vankka.dependencydownload.task;

import dev.vankka.dependencydownload.DependencyDownloadGradlePlugin;
import dev.vankka.dependencydownload.inputs.ResourceSplittingStrategy;
import dev.vankka.dependencydownload.common.util.HashUtil;
import dev.vankka.dependencydownload.Dependency;
import dev.vankka.dependencydownload.inputs.Relocation;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class GenerateDependencyDownloadResourceTask extends DefaultTask {

    @Input
    public abstract Property<Configuration> getConfiguration();

    @OutputDirectory
    public abstract RegularFileProperty getFileLocation();

    @Input
    public abstract Property<String> getFile();

    @Input
    public abstract Property<Boolean> getIncludeShadowJarRelocations();

    @Input
    public abstract Property<String> getHashingAlgorithm();

    @Input
    public abstract Property<ResourceSplittingStrategy> getResourceSplittingStrategy();

    private final List<Relocation> relocations = new ArrayList<>();

    private static final String DEFAULT_FILE = "";

    @Inject
    public GenerateDependencyDownloadResourceTask(ObjectFactory factory) {
        getConfiguration().convention(
                getProject().getConfigurations().getByName(DependencyDownloadGradlePlugin.BASE_CONFIGURATION_NAME));
        getFileLocation().convention(getConfiguration().flatMap(conf -> factory.fileProperty().fileValue(getResourceDirectory(conf))));
        getFile().convention(DEFAULT_FILE);
        getIncludeShadowJarRelocations().convention(true);
        getHashingAlgorithm().convention("SHA-256");
        getResourceSplittingStrategy().convention(ResourceSplittingStrategy.SINGLE_FILE);
    }

    //
    // Relocations
    //

    @SuppressWarnings("unused") // API
    public GenerateDependencyDownloadResourceTask relocate(String pattern, String destination) {
        return relocate(pattern, destination, null);
    }

    public GenerateDependencyDownloadResourceTask relocate(String pattern, String destination, Action<Relocation> configure) {
        Relocation relocation = new Relocation(pattern, destination, new ArrayList<>(), new ArrayList<>());
        addRelocator(relocation, configure);
        return this;
    }

    @SuppressWarnings("unused") // API
    public GenerateDependencyDownloadResourceTask relocate(Relocation relocation) {
        addRelocator(relocation, null);
        return this;
    }

    @SuppressWarnings("unused") // API
    public GenerateDependencyDownloadResourceTask relocate(Class<? extends Relocation> relocationClass)
            throws ReflectiveOperationException {
        return relocate(relocationClass, null);
    }

    public <R extends Relocation> GenerateDependencyDownloadResourceTask relocate(Class<R> relocatorClass, Action<R> configure)
            throws ReflectiveOperationException {
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
        getFileLocation().convention(getProject().getObjects().fileProperty().fileValue(getResourceDirectory(configuration)));
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
        Path resourcesDirectory = getResourceDirectory(getConfiguration().get()).toPath();
        Path fileLocation = getFileLocation().get().getAsFile().toPath();
        String hashingAlgorithm = getHashingAlgorithm().get();
        ResourceSplittingStrategy splittingStrategy = getResourceSplittingStrategy().get();
        boolean single = splittingStrategy == ResourceSplittingStrategy.SINGLE_FILE;
        boolean topLevel = splittingStrategy == ResourceSplittingStrategy.TOP_LEVEL_DEPENDENCIES;
        boolean all = splittingStrategy == ResourceSplittingStrategy.ALL_DEPENDENCIES;

        Property<Configuration> property = getConfiguration();
        Configuration configuration;
        if (property.isPresent()) {
            configuration = property.get();
        } else {
            throw new IllegalArgumentException("configuration must be provided");
        }

        List<Dependency> dependencies = single ? new ArrayList<>() : null;
        for (Configuration config : getConfigurations(configuration)) {
            for (ResolvedDependency resolvedDependency : config.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
                if (topLevel) {
                    dependencies = new ArrayList<>();
                }
                for (Dependency dependency : processDependency(resolvedDependency, hashingAlgorithm)) {
                    if (!all) {
                        dependencies.add(dependency);
                        continue;
                    }

                    // Make a file for all distinct dependencies
                    makeFile(
                            resourcesDirectory,
                            fileLocation,
                            hashingAlgorithm,
                            splittingStrategy,
                            configuration,
                            Collections.singletonList(dependency)
                    );
                }
                if (topLevel) {
                    // Make a file for all top level dependencies
                    makeFile(
                            resourcesDirectory,
                            fileLocation,
                            hashingAlgorithm,
                            splittingStrategy,
                            configuration,
                            dependencies
                    );
                }
            }
        }

        if (single) {
            // Make a single file for all dependencies in the configuration
            makeFile(
                    resourcesDirectory,
                    fileLocation,
                    hashingAlgorithm,
                    splittingStrategy,
                    configuration,
                    dependencies
            );
        }
    }

    private void makeFile(
            Path resourcesDirectory,
            Path fileLocation,
            String hashingAlgorithm,
            ResourceSplittingStrategy splittingStrategy,
            Configuration configuration,
            List<Dependency> dependencies
    ) throws IOException {
        if (dependencies.isEmpty()) {
            // Don't write empty files
            getLogger().warn("Attempted to create dependency file with no dependencies");
            return;
        }

        StringJoiner result = new StringJoiner("\n");
        result.add("===ALGORITHM " + hashingAlgorithm);

        dependencies.forEach(dependency -> result.add(dependency.toString()));

        List<Relocation> relocations = new ArrayList<>();
        if (getIncludeShadowJarRelocations().get()) {
            getShadowJarRelocations(relocations);
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
        if (!Files.exists(resourcesDirectory)) {
            Files.createDirectories(resourcesDirectory);
        }

        String file = getFile().get();
        if (file.equals(DEFAULT_FILE)) {
            switch (splittingStrategy) {
                default:
                case SINGLE_FILE:
                    file = configuration.getName() + ".txt";
                    break;
                case TOP_LEVEL_DEPENDENCIES:
                case ALL_DEPENDENCIES:
                    Dependency dependency = dependencies.get(0);
                    String classifier = dependency.getClassifier();
                    file = dependency.getGroup() + ":" + dependency.getModule()
                            + (classifier != null ? "-" + classifier : "") + ".txt";
                    break;
            }
        } else if (splittingStrategy != ResourceSplittingStrategy.SINGLE_FILE) {
            Dependency dependency = dependencies.get(0);
            file = file
                    .replace("%group%", dependency.getGroup())
                    .replace("%module%", dependency.getModule())
                    .replace("%version%", dependency.getVersion())
                    .replace("%classifier%", dependency.getClassifier() != null ? dependency.getClassifier() : "")
                    .replace("%hash%", dependency.getHash());
        }

        Path dependenciesFile = fileLocation.resolve(file);
        if (Files.exists(dependenciesFile)) {
            Files.delete(dependenciesFile);
        } else {
            // Create parent directory if it doesn't exist
            Files.createDirectories(dependenciesFile.getParent());
        }
        Files.createFile(dependenciesFile);

        try (FileWriter writer = new FileWriter(dependenciesFile.toFile())) {
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
    private void getShadowJarRelocations(List<Relocation> relocations) {
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

    private List<Dependency> processDependency(ResolvedDependency dependency, String hashingAlgorithm) throws NoSuchAlgorithmException, IOException {
        String hash = null;
        String snapshotVersion = null;
        String classifier = null;
        for (ResolvedArtifact moduleArtifact : dependency.getModuleArtifacts()) {
            if (!moduleArtifact.getType().equals("jar")) {
                continue;
            }

            String currentClassifier = moduleArtifact.getClassifier();
            if (currentClassifier != null) {
                classifier = currentClassifier;
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

        List<Dependency> dependencies = new ArrayList<>();
        if (hash != null) {
            String group = dependency.getModuleGroup();
            String module = dependency.getModuleName();
            String version = dependency.getModuleVersion();
            String finalVersion = snapshotVersion != null ? version + ":" + snapshotVersion : version;
            if (finalVersion.endsWith("-SNAPSHOT")) {
                Logger logger = getLogger();
                logger.warn("");
                logger.warn(group + ":" + module + " resolved to a non-versioned snapshot version: " + version);
                logger.warn("This is usually caused by the dependency being resolved from mavenLocal()");
                logger.warn("and the local repository containing the dependency with the version '" + version + "' (without a timestamp)");
            }
            dependencies.add(new Dependency(group, module, finalVersion, classifier, hash));
        }

        for (ResolvedDependency child : dependency.getChildren()) {
            dependencies.addAll(
                    processDependency(child, hashingAlgorithm)
            );
        }
        return dependencies;
    }
}
