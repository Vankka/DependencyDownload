package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.task.GenerateDependencyDownloadResourceTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.TaskContainer;

public class DependencyDownloadGradlePlugin implements Plugin<Project> {

    public static final String BASE_CONFIGURATION_NAME = "runtimeDownloadOnly";
    public static final String COMPILE_CONFIGURATION_NAME = "runtimeDownload";

    @Override
    public void apply(Project project) {
        ConfigurationContainer configurations = project.getConfigurations();

        Configuration baseConfiguration = configurations.create(BASE_CONFIGURATION_NAME);
        Configuration compileConfiguration = configurations.create(COMPILE_CONFIGURATION_NAME);
        configurations.getByName("compileOnly").extendsFrom(compileConfiguration);

        TaskContainer tasks = project.getTasks();
        String taskName = "generateRuntimeDownloadResourceFor";
        tasks.register(taskName + "RuntimeDownloadOnly", GenerateDependencyDownloadResourceTask.class, t -> t.convention(baseConfiguration));
        tasks.register(taskName + "RuntimeDownload", GenerateDependencyDownloadResourceTask.class, t -> t.convention(compileConfiguration));
    }
}
