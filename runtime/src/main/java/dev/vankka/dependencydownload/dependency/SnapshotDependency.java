package dev.vankka.dependencydownload.dependency;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SnapshotDependency extends StandardDependency {

    private final String snapshotVersion;

    public SnapshotDependency(
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String snapshotVersion,
            String hash,
            String hashingAlgorithm
    ) {
        super(groupId, artifactId, version, classifier, hash, hashingAlgorithm);
        this.snapshotVersion = snapshotVersion;
    }

    @Override
    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    @Override
    public boolean isSnapshot() {
        return true;
    }

    @Override
    public @NotNull String getFileName() {
        String classifier = getClassifier();
        return getArtifactId()
                + '-' + getSnapshotVersion()
                + (classifier != null ? '-' + classifier : "")
                + ".jar";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SnapshotDependency that = (SnapshotDependency) o;
        return Objects.equals(snapshotVersion, that.snapshotVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), snapshotVersion);
    }
}
