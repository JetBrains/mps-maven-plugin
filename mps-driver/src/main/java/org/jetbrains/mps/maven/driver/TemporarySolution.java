package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;

class TemporarySolution implements Serializable {
    final File modelsDirectory;
    final File outputDirectory;

    TemporarySolution(File modelsDirectory, File outputDirectory) {
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
    }
}
