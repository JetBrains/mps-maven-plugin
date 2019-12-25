package org.jetbrains.mps.maven.driver;

import jetbrains.mps.project.io.DescriptorIOException;
import jetbrains.mps.tool.builder.make.GeneratorWorker;
import jetbrains.mps.tool.common.JavaCompilerProperties;
import jetbrains.mps.tool.common.Script;
import org.apache.log4j.Level;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Entry point for executing code in the context of MPS classloaders.
 * Main class executed by {@code org.jetbrains.mps.mavenplugin.Mps}.
 */
public class Driver {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected exactly one argument, got " + args.length);
            System.exit(1);
        }
        String inputFileName = args[0];

        GeneratorInput input;
        try (InputStream is = new FileInputStream(inputFileName);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            input = (GeneratorInput) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }

        TemporarySolution solution = new TemporarySolution(input.modelsDirectory, input.outputDirectory);
        Path solutionFile;
        try {
            solutionFile = Files.createTempFile("mpsmaven", ".msd");
            TemporarySolutionIO.writeToFile(solution, solutionFile);
        } catch (IOException | DescriptorIOException e) {
            System.err.println("Error creating temporary file");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }

        Script script = toScript(solutionFile, input);

        GeneratorWorker worker = new GeneratorWorker(script);

        // workFromMain calls System.exit() with appropriate exit code.
        worker.workFromMain();
    }

    private static Script toScript(Path solutionFile, GeneratorInput generatorInput) {
        Script script = new Script();
        script.updateLogLevel(Level.TRACE);

        // Leave compilation to Maven. We can do it since we generate one module at a time, and it lets us (and the
        // plugin user) control the compilation process.
        //
        // Also, enabling the Java compilation currently makes the build of hello-world sample fail, reasons not yet
        // investigated.
        new JavaCompilerProperties(script).setSkipCompilation(true);

        addLibraryJarsToScript(script, generatorInput.libraryJars);

        script.addChunk(Collections.singletonList(solutionFile.toAbsolutePath().toString()), false);
        return script;
    }

    private static void addLibraryJarsToScript(Script script, Iterable<File> libraryJars) {
        for (File file : libraryJars) {
            script.addLibraryJar(file.getAbsolutePath());
        }
    }
}
