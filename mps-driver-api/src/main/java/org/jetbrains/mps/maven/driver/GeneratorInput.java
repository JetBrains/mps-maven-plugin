package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class GeneratorInput implements Serializable {
    public final File workingDirectory;
    public final File modelsDirectory;
    public final File outputDirectory;
    public final Collection<File> libraryJars;
    public final Map<String, File> classStubsJars;

    public GeneratorInput(
            File workingDirectory,
            File modelsDirectory,
            File outputDirectory,
            Collection<File> libraryJars,
            Map<String, File> classStubsJars) {
        this.workingDirectory = workingDirectory;
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
        this.libraryJars = libraryJars;
        this.classStubsJars = classStubsJars;
    }
}
