package dev.vankka.dependencydownload.dependency;

public class SnapshotDependency extends StandardDependency {

    private final String snapshotVersion;

    public SnapshotDependency(String groupId, String artifactId, String version, String snapshotVersion, String hash, String hashingAlgorithm) {
        super(groupId, artifactId, version, hash, hashingAlgorithm);
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
    public String getFileName() {
        return getArtifactId() + '-' + getSnapshotVersion() + ".jar";
    }
}
