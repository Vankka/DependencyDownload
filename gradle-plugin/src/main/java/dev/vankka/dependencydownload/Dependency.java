package dev.vankka.dependencydownload;

import java.util.Objects;

public class Dependency {

    private final String group;
    private final String module;
    private final String version;
    private final String classifier;
    private final String hash;

    public Dependency(String group, String module, String version, String classifier, String hash) {
        this.group = group;
        this.module = module;
        this.version = version;
        this.classifier = classifier;
        this.hash = hash;
    }

    public String getGroup() {
        return group;
    }

    public String getModule() {
        return module;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return group + ":" + module + ":" + version + (classifier != null ? ":" + classifier : "") + " " + hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return group.equals(that.group) && module.equals(that.module) && version.equals(
                that.version) && Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, module, version, classifier);
    }
}
