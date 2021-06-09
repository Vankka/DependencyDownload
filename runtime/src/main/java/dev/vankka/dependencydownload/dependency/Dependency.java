package dev.vankka.dependencydownload.dependency;

/**
 * A maven dependency.
 */
public interface Dependency {

    String MAVEN_PATH_FORMAT = "%s/%s/%s/%s";

    /**
     * The group id of the dependency.
     * @return the dependency's group id
     */
    String getGroupId();

    /**
     * The artifact id of the dependency.
     * @return the dependency's artifact id
     */
    String getArtifactId();

    /**
     * The version of the dependency.
     * @return the dependency's version
     */
    String getVersion();

    /**
     * The hash of the dependency, this is checked against the downloaded file.
     * @return the hash of the dependency archive
     * @see #getHashingAlgorithm()
     */
    String getHash();

    /**
     * The hashing algorithm used for the {@link #getHash()}.
     * @return the hashing algorithm used for the dependency archive's hash
     * @see #getHash()
     */
    String getHashingAlgorithm();

    /**
     * Returns the file name for the end of the maven path.
     * @return the file name for the dependency
     */
    default String getFileName() {
        return getArtifactId() + '-' + getVersion() + ".jar";
    }

    /**
     * The path to this dependency on a maven repository, without the protocol, domain or slash at the beginning.
     * @return the path to this dependency's jar file on a maven repository
     */
    default String getMavenPath() {
        return String.format(
                MAVEN_PATH_FORMAT,
                getGroupId().replace('.', '/'),
                getArtifactId(),
                getVersion(),
                getFileName()
        );
    }

    /**
     * Gets the group id, artifact id and version seperated by semicolons.
     * @return the maven artifact's GAV parameters seperated by semicolons (<code>:</code>)
     */
    default String getMavenArtifact() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }
}
