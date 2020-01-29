package org.jetbrains.mps.maven.driver;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class GeneratorInput implements Serializable {
    public final File workingDirectory;
    public final String moduleName;
    public final File modelsDirectory;
    public final Collection<String> sourceRootDirectories;
    public final File outputDirectory;
    public final Collection<File> libraryJars;
    public final Map<String, File> classStubsJars;

    public GeneratorInput(
            File workingDirectory,
            String moduleName,
            File modelsDirectory,
            Collection<String> sourceRootDirectories,
            File outputDirectory,
            Collection<File> libraryJars,
            Map<String, File> classStubsJars) {
        this.workingDirectory = workingDirectory;
        this.moduleName = moduleName;
        this.modelsDirectory = modelsDirectory;
        this.sourceRootDirectories = sourceRootDirectories;
        this.outputDirectory = outputDirectory;
        this.libraryJars = libraryJars;
        this.classStubsJars = classStubsJars;
    }
}
