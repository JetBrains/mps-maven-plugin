package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

class TemporarySolution implements Serializable {
    final File modelsDirectory;
    final Collection<File> javaSourceDirs;
    final File outputDirectory;

    TemporarySolution(File baseDirectory, File modelsDirectory, Collection<String> javaSources, File outputDirectory) {
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;

        javaSourceDirs = new ArrayList<>();
        for (String sourceRoot: javaSources) {
            javaSourceDirs.add(new File(baseDirectory, sourceRoot));
        }
    }
}
