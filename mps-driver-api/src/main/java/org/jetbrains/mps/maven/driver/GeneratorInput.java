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
    public final Map<String, File> javaClassJars;

    public GeneratorInput(File workingDirectory, File modelsDirectory, File outputDirectory, Collection<File> libraryJars, Map<String, File> javaClassJars) {
        this.workingDirectory = workingDirectory;
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
        this.libraryJars = libraryJars;
        this.javaClassJars = javaClassJars;
    }

//    public class NamedFile {
//        public final String name;
//        public final File file;
//
//        public NamedFile(String name, File file) {
//            this.name = name;
//            this.file = file;
//        }
//    }
}
