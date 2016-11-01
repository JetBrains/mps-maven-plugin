package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

public class GeneratorInput implements Serializable {
    public final File workingDirectory;
    public final File modelsDirectory;
    public final File outputDirectory;
    public final Collection<File> libraryJars;

    public GeneratorInput(File workingDirectory, File modelsDirectory, File outputDirectory, Collection<File> libraryJars) {
        this.workingDirectory = workingDirectory;
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
        this.libraryJars = libraryJars;
    }
}
