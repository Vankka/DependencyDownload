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

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MavenDependency implements Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String hash;
    private final String hashingAlgorithm;
    private final String snapshotVersion;

    public MavenDependency(
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @Nullable String classifier,
            @NotNull String hash,
            @NotNull String hashingAlgorithm
    ) {
        this(groupId, artifactId, version, classifier, null, hash, hashingAlgorithm);
    }

    public MavenDependency(
            @NotNull String groupId,
            @NotNull String artifactId,
            @NotNull String version,
            @Nullable String classifier,
            @Nullable @Pattern("[0-9]{8}\\.[0-9]{6}-[0-9]+") String snapshotVersion,
            @NotNull String hash,
            @NotNull String hashingAlgorithm
    ) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.snapshotVersion = snapshotVersion;
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
        return snapshotVersion;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenDependency that = (MavenDependency) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version)
                && Objects.equals(classifier, that.classifier)
                && Objects.equals(snapshotVersion, that.snapshotVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier, snapshotVersion);
    }
}
