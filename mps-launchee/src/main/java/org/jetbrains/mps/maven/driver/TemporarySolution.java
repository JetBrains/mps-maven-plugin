package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;

public class TemporarySolution implements Serializable {
    public final File modelsDirectory;
    public final File outputDirectory;

    public TemporarySolution(File modelsDirectory, File outputDirectory) {
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
    }
}
