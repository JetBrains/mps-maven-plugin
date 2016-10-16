package org.jetbrains.mps.mavenplugin.mps;

import com.google.common.collect.Lists;
import jetbrains.mps.tool.builder.make.GeneratorWorker;
import jetbrains.mps.tool.common.JavaCompilerProperties;
import jetbrains.mps.tool.common.Script;
import org.apache.log4j.Level;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

public class Mps {
    public static void runGenerator(Log log, GeneratorInput generatorInput) {
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
}
