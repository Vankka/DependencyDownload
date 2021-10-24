package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.task.GenerateDependencyDownloadResourceTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskContainer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DependencyDownloadGradlePlugin implements Plugin<Project> {

    public static final String BASE_CONFIGURATION_NAME = "runtimeDownloadOnly";
    public static final String COMPILE_CONFIGURATION_NAME = "runtimeDownload";

    @Override
    public void apply(Project project) {
        //
        // Configurations
        //
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration baseConfiguration = configurations.create(BASE_CONFIGURATION_NAME);

        Configuration compileConfiguration = configurations.create(COMPILE_CONFIGURATION_NAME);
        configurations.getByName("compileOnly").extendsFrom(compileConfiguration);

        baseConfiguration.extendsFrom(compileConfiguration);

        //
        // Tasks
        //
        Map<String, Configuration> tasksToMake = new HashMap<>();
        String taskName = "generateRuntimeDownloadResourceFor";
        tasksToMake.put(taskName + "RuntimeDownloadOnly", baseConfiguration);
        tasksToMake.put(taskName + "RuntimeDownload", compileConfiguration);

        TaskContainer tasks = project.getTasks();
        for (Map.Entry<String, Configuration> entry : tasksToMake.entrySet()) {
            String configurationName = entry.getKey();
            Configuration configuration = entry.getValue();
            tasks.register(configurationName, GenerateDependencyDownloadResourceTask.class, t -> t.configuration(configuration));
        }
    }
}
