package org.jetbrains.mps.mavenplugin.mps;

import java.io.File;
import java.util.Collection;

class GeneratorInput {
    final File solutionFile;
    final Collection<File> libraryJars;

    GeneratorInput(File solutionFile, Collection<File> libraryJars) {
        this.solutionFile = solutionFile;
        this.libraryJars = libraryJars;
    }
}
