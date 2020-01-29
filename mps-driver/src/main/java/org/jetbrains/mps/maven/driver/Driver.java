package org.jetbrains.mps.maven.driver;

import jetbrains.mps.project.io.DescriptorIOException;
import jetbrains.mps.tool.builder.make.GeneratorWorker;
import jetbrains.mps.tool.common.JavaCompilerProperties;
import jetbrains.mps.tool.common.Script;
import org.apache.log4j.Level;
import org.jetbrains.mps.openapi.persistence.ModelRootFactory;
import org.jetbrains.mps.openapi.persistence.PersistenceFacade;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

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

        TemporarySolution solution = new TemporarySolution(input.moduleName, input.modelsDirectory, input.sourceRootDirectories, input.outputDirectory);
        Path solutionFile;
        try {
            solutionFile = Files.createTempFile("mpsmaven", ".msd");
            solutionFile.toFile().deleteOnExit();
            TemporarySolutionIO.writeToFile(solution, solutionFile);
        } catch (IOException | DescriptorIOException e) {
            System.err.println("Error creating temporary file");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }

        Script script = toScript(solutionFile, input);

        // TODO introduce proper support for mps core plugins
        GeneratorWorker worker = new GeneratorWorker(script) {
            @Override
            public void work() {

                try {
                    Class<?> modelRootFactoryClass = Class.forName("jetbrains.mps.java.core.sourceStubs.JavaSourceStubModelRootFactory");
                    PersistenceFacade.getInstance().setModelRootFactory(
                            TemporarySolutionIO.JAVA_SOURCE_STUBS_TYPE,
                            (ModelRootFactory) modelRootFactoryClass.newInstance()
                    );
                } catch (ClassNotFoundException e) {
                    System.out.println("Java source persistence class not found");
                } catch (InstantiationException | IllegalAccessException e) {
                    System.out.println("Could not instantiate java source stubs persistence support");
                }

                super.work();
            }
        };

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
        addJavaStubSolutionsToScript(script, generatorInput.classStubsJars);

        script.addChunk(Collections.singletonList(solutionFile.toAbsolutePath().toString()), false);
        return script;
    }

    private static void addLibraryJarsToScript(Script script, Iterable<File> libraryJars) {
        for (File file : libraryJars) {
            script.addLibraryJar(file.getAbsolutePath());
        }
    }

    private static void addJavaStubSolutionsToScript(Script script, Map<String, File> jars) {
        try {
            for (Map.Entry<String, File> entry : jars.entrySet()) {
                String libraryName = entry.getKey();
                File jarFile = entry.getValue();

                File stubSolutionDescriptorFile = Files.createTempFile("mpsmavenstub", ".msd").toFile();
                stubSolutionDescriptorFile.deleteOnExit();

                TemporarySolutionIO.createStubSolution(stubSolutionDescriptorFile, libraryName, jarFile);
                script.addLibrary(libraryName, stubSolutionDescriptorFile);
            }
        } catch (IOException | DescriptorIOException e) {
            System.err.println("Error creating temporary file");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }
}
