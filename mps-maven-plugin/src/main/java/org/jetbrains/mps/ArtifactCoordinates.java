package org.jetbrains.mps;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ArtifactCoordinates {
    @NotNull
    final String groupId;
    @NotNull
    final String artifactId;
    @NotNull
    final String classifier;
    @NotNull
    final String extension;
    @NotNull
    final String version;

    ArtifactCoordinates(@Nullable String groupId, @Nullable String artifactId, @Nullable String classifier,
                        @Nullable String extension, @Nullable String version) {
        this.groupId = Strings.nullToEmpty(groupId);
        this.artifactId = Strings.nullToEmpty(artifactId);
        this.classifier = Strings.nullToEmpty(classifier);
        this.extension = Strings.nullToEmpty(extension);
        this.version = Strings.nullToEmpty(version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactCoordinates artifact = (ArtifactCoordinates) o;

        if (!groupId.equals(artifact.groupId)) return false;
        if (!artifactId.equals(artifact.artifactId)) return false;
        if (!classifier.equals(artifact.classifier)) return false;
        if (!extension.equals(artifact.extension)) return false;
        return version.equals(artifact.version);

    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + classifier.hashCode();
        result = 31 * result + extension.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(groupId);
        buffer.append(':').append(artifactId);
        buffer.append(':').append(extension);
        if (!classifier.isEmpty()) {
            buffer.append(':').append(classifier);
        }
        buffer.append(':').append(version);
        return buffer.toString();
    }
}
