package dev.vankka.dependencydownload.dependency;

public class StandardDependency implements Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String hash;
    private final String hashingAlgorithm;

    public StandardDependency(String groupId, String artifactId, String version, String hash, String hashingAlgorithm) {
        this(groupId, artifactId, version, null, hash, hashingAlgorithm);
    }

    public StandardDependency(String groupId, String artifactId, String version, String classifier, String hash, String hashingAlgorithm) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.hash = hash;
        this.hashingAlgorithm = hashingAlgorithm;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public boolean asClassifier() {
        return classifier != null;
    }

    @Override
    public String getSnapshotVersion() {
        return null;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    @Override
    public boolean isSnapshot() {
        return false;
    }
}
