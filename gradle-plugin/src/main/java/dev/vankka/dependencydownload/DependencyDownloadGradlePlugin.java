/*
 * MIT License
 *
 * Copyright (c) 2021-2025 Vankka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.vankka.dependencydownload;

import dev.vankka.dependencydownload.task.GenerateDependencyDownloadResourceTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.TaskContainer;

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
