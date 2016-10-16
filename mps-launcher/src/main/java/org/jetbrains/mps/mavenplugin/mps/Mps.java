package org.jetbrains.mps.mavenplugin.mps;

import com.google.common.collect.Lists;
import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.tool.builder.make.GeneratorWorker;
import jetbrains.mps.tool.common.JavaCompilerProperties;
import jetbrains.mps.tool.common.Script;
import org.apache.log4j.Level;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Mps {
    private static void runGenerator(Log log, GeneratorInput generatorInput) {
        Script script = toScript(generatorInput);
        new GeneratorWorker(script, new MpsToMavenLogger(log)).work();
    }

    private static Script toScript(GeneratorInput generatorInput) {
        Script script = new Script();
        script.updateLogLevel(Level.TRACE);

        // Disable loading bootstrap libraries, otherwise MPS looks for them in some lib folder under some home path
        // and the plugin doesn't provide anything like that, so it fails.
        script.setLoadBootstrapLibraries(false);

        // Leave compilation to Maven. We can do it since we generate one module at a time, and it lets us (and the
        // plugin user) control the compilation process.
        new JavaCompilerProperties(script).setSkipCompilation(true);

        addLibraryJarsToScript(script, generatorInput.libraryJars);

        script.addChunk(Lists.newArrayList(generatorInput.solutionFile.getAbsolutePath()), false);
        return script;
    }

    private static void addLibraryJarsToScript(Script script, Iterable<File> libraryJars) {
        for (File file : libraryJars) {
            script.addLibraryJar(file.getAbsolutePath());
        }
    }

    public static void launchMps(Iterable<File> extractedDependencies, File temporaryWorkingDirectory, TemporarySolution temporarySolution, Log mavenLog) throws MojoExecutionException {
        MavenLogAppender.startPluginLog(mavenLog);
        try {
            Collection<MpsModule> modules = readModules(extractedDependencies);

            List<File> libraries = getLibraries(modules);

            File solutionFile = new File(temporaryWorkingDirectory, "solution.msd");
            temporarySolution.writeToFile(solutionFile);

            GeneratorInput generatorInput = new GeneratorInput(solutionFile, libraries);
            runGenerator(mavenLog, generatorInput);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            MavenLogAppender.endPluginLog();
        }
    }

    private static Collection<MpsModule> readModules(Iterable<File> extractedDependencies) {
        try (MultiMiner miner = new MultiMiner()) {
            ArrayList<MpsModule> modules = new ArrayList<>();
            for (File root : extractedDependencies) {
                Collection<ModulesMiner.ModuleHandle> moduleHandles = miner.collectModules(root);
                modules.add(MpsModules.fromRootAndModuleHandles(root, moduleHandles));
            }
            return modules;
        }
    }

    private static List<File> getLibraries(Collection<MpsModule> values) {
        return values.stream().flatMap(m -> m.libraries.stream()).collect(Collectors.toList());
    }
}
