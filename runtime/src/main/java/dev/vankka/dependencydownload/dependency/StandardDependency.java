package dev.vankka.dependencydownload.dependency;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StandardDependency implements Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String hash;
    private final String hashingAlgorithm;

    public StandardDependency(String groupId, String artifactId, String version, String classifier, String hash, String hashingAlgorithm) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.hash = hash;
        this.hashingAlgorithm = hashingAlgorithm;
    }

    @Override
    public @NotNull String getGroupId() {
        return groupId;
    }

    @Override
    public @NotNull String getArtifactId() {
        return artifactId;
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getSnapshotVersion() {
        return null;
    }

    @Override
    public @NotNull String getHash() {
        return hash;
    }

    @Override
    public @NotNull String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    @Override
    public boolean isSnapshot() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardDependency that = (StandardDependency) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version)
                && Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier);
    }
}
