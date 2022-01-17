# DependencyDownload
![Maven Central](https://img.shields.io/maven-central/v/dev.vankka.dependencydownload/runtime?label=release)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dev.vankka.dependencydownload/runtime?label=dev&server=https%3A%2F%2Fs01.oss.sonatype.org)
![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/dev.vankka.dependencydownload.plugin?label=gradle%20plugin)

A library to download, relocate & load dependencies during runtime.
There is also a Gradle plugin to generate a metadata file, to avoid having to define the dependencies in code

Uses [jar-relocator](https://github.com/lucko/jar-relocator/) for relocations during runtime  
Looking for something to use with Minecraft? [Check out MinecraftDependencyDownload](https://github.com/Vankka/MinecraftDependencyDownload/)

## Dependency
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'dev.vankka.dependencydownload:runtime:1.0.2'
}
```

<details>
    <summary>Snapshots</summary>

```groovy
repositories {
    maven {
        url 'https://s01.oss.sonatype.org/content/repositories/snapshots/'
    }
}

dependencies {
    implementation 'dev.vankka.dependencydownload:runtime:1.0.3-SNAPSHOT'
}
```
</details>

## Usage
```java

DependencyManager manager = new DependencyManager(new File("cache").toPath());
manager.addDependency(new StandardDependency("com.example", "examplepackage", "1.0.0", "<hash>", "SHA-256"));
manager.addRelocation(new Relocation("com.example", "relocated.com.example"));

Executor executor = Executors.newCachedThreadPool();

// All of these return CompletableFuture it is important to let the previous step finishing before starting the next
manager.downloadAll(executor, Collections.singletonList(new StandardRepository("https://repo.example.com/maven2"))).join();
manager.relocateAll(executor).join();
manager.loadAll(executor, classpathAppender).join(); // ClasspathAppender is a interface that you need to implement to append a Path to the classpath
```

## Gradle plugin
```groovy
plugins {
    id 'dev.vankka.dependencydownload.plugin' version '1.0.2'
}

dependencies {
    runtimeDownload 'some:dependency:x.y.z'
    runtimeDownloadOnly 'some.other:dependency:x.y.z'
}

jar.dependsOn generateRuntimeDownloadResourceForRuntimeDownloadOnly, generateRuntimeDownloadResourceForRuntimeDownload
```
This would generate two files `runtimeDownloadOnly.txt` and `runtimeDownload.txt`

### ShadowJar support
[shadow](https://github.com/johnrengelman/shadow) is supported to include the relocations in the generated metadata files

### Loading dependencies from a plugin generated file
```java
DependecyManager manager = new DependencyManager();
manager.loadFromResource(getClass().getResource("runtimeDownloadOnly.txt"));
```

### Customizing 
```groovy
import dev.vankka.dependencydownload.task.GenerateDependencyDownloadResourceTask

task generateResource(type: GenerateDependencyDownloadResourceTask) {
    configuration = project.configurations.runtimeDownload
    includeRelocations = true
    hashingAlgorithm = 'SHA-256'
    file = 'dependencies.txt'
}
```

## Download `jar-relocator` during runtime
Bring the jar minifying to the next extreme
```groovy
import dev.vankka.dependencydownload.task.GenerateDependencyDownloadResourceTask
plugins {
    id 'dev.vankka.dependencydownload.plugin' version '1.0.2'
}

configurations {
    jarRelocator
}

repositories {
    mavenCentral()
}

dependencies {
    implementation('dev.vankka.dependencydownload:runtime:1.0.2') {
        exclude module: 'jar-relocator'
    }
    jarRelocator 'me.lucko:jar-relocator:1.4'
}

task generateJarRelocatorResource(type: GenerateDependencyDownloadResourceTask) {
    configuration = project.configurations.jarRelocator
}
processResources.dependsOn generateJarRelocatorResource
```

```java
DependecyManager manager = new DependencyManager();
manager.loadFromResource(getClass().getResource("jarRelocator.txt"));

Executor executor = Executors.newCachedThreadPool(2);

manager.downloadAll(executor, Collections.singletonList(new StandardRepository("https://repo.example.com/maven2"))).join();
manager.loadAll(executor, classpathAppender).join();
// now jar-relocator is in the classpath and we can load (and relocate) dependencies from a regular configuration
```
