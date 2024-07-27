/*
 * MIT License
 *
 * Copyright (c) 2021-2024 Vankka
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

package dev.vankka.dependencydownload.dependency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A maven dependency.
 */
public interface Dependency {

    String MAVEN_PATH_FORMAT = "%s/%s/%s/%s";

    /**
     * The group id of the dependency.
     * @return the dependency's group id
     */
    @NotNull
    String getGroupId();

    /**
     * The artifact id of the dependency.
     * @return the dependency's artifact id
     */
    @NotNull
    String getArtifactId();

    /**
     * The version of the dependency.
     * @return the dependency's version
     */
    @NotNull
    String getVersion();

    /**
     * The classifier for the dependency artifact, if any.
     * @return the dependency artifact's classifier or {@code null}.
     */
    @Nullable
    String getClassifier();

    /**
     * The timestamped snapshot version.
     * @return the timestamped snapshot version or {@code null} if this isn't a snapshot dependency.
     * @see #isSnapshot()
     */
    @Nullable
    String getSnapshotVersion();

    /**
     * The hash of the dependency, this is checked against the downloaded file.
     * @return the hash of the dependency archive
     * @see #getHashingAlgorithm()
     */
    @NotNull
    String getHash();

    /**
     * The hashing algorithm used for the {@link #getHash()}.
     * @return the hashing algorithm used for the dependency archive's hash
     * @see #getHash()
     */
    @NotNull
    String getHashingAlgorithm();

    /**
     * If this is a snapshot dependency.
     * @return true if this dependency is a snapshot
     */
    default boolean isSnapshot() {
        return getSnapshotVersion() != null;
    }

    /**
     * Returns the file name for the end of the maven path.
     * @return the file name for the dependency
     */
    @NotNull
    default String getFileName() {
        String classifier = getClassifier();
        String snapshotVersion = getSnapshotVersion();
        return getArtifactId()
                + '-' + (snapshotVersion != null ? snapshotVersion : getVersion())
                + (classifier != null ? '-' + classifier : "")
                + ".jar";
    }

    /**
     * Returns the file name when stored to disk.
     * @return the file name for storing the dependency
     */
    @NotNull
    default String getStoredFileName() {
        String classifier = getClassifier();
        return getGroupId()
                + '-' + getArtifactId()
                + '-' + getVersion()
                + (isSnapshot() ? "-" + getSnapshotVersion() : "")
                + (classifier != null ? '-' + classifier : "")
                + ".jar";
    }

    /**
     * The path to this dependency on a maven repository, without the protocol, domain or slash at the beginning.
     * @return the path to this dependency's jar file on a maven repository
     */
    @NotNull
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
     * Gets the group id, artifact id, version and classifier (if specified) seperated by semicolons.
     * @return the maven artifact's GAV and classifier parameters seperated by semicolons (<code>:</code>)
     */
    @NotNull
    default String getMavenArtifact() {
        String classifier = getClassifier();
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + (classifier != null ? ":" + classifier : "");
    }
}
