package org.jetbrains.mps.mavenplugin.mps;

import java.io.File;
import java.util.Collection;

public class GeneratorInput {
    public final File solutionFile;
    public final Collection<File> libraryJars;

    public GeneratorInput(File solutionFile, Collection<File> libraryJars) {
        this.solutionFile = solutionFile;
        this.libraryJars = libraryJars;
    }
}
