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
