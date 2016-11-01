package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.tool.common.GeneratorInput;
import jetbrains.mps.tool.common.GeneratorStartupInfo;
import org.apache.commons.exec.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Mps2 {
    private static final String[] DEFAULT_JVM_ARGS = new String[]{"-Xss1024k", "-Xmx512m", "-XX:MaxPermSize=92m", "-XX:+HeapDumpOnOutOfMemoryError"};

    public static void launchMps(GeneratorStartupInfo generatorStartupInfo, GeneratorInput input, Log mavenLog) throws MojoExecutionException, MojoFailureException {
        CommandLine commandLine = getCommandLine(generatorStartupInfo, input);
        DefaultExecutor executor = new DefaultExecutor();

        LogOutputStream stream = new LogOutputStream() {
            @Override
            protected void processLine(String line, int logLevel) {
                mavenLog.info(line);
            }
        };

        executor.setWorkingDirectory(input.workingDirectory);
        executor.setStreamHandler(new PumpStreamHandler(stream));
        try {
            if (mavenLog.isDebugEnabled()) {
                mavenLog.debug("Executing " + commandLine);
            }
            executor.execute(commandLine);
        } catch (ExecuteException e) {
            throw new MojoFailureException("Process exited with error code " + e.getExitValue(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not execute command " + commandLine, e);
        }
    }

    private static CommandLine getCommandLine(GeneratorStartupInfo generatorStartupInfo, GeneratorInput input) throws MojoExecutionException, MojoFailureException {
        Collection<File> classPaths = new ArrayList<>();
        classPaths.add(getToolsJar());
        classPaths.addAll(calculateMpsClassPath(generatorStartupInfo.mpsHome));
        classPaths.addAll(generatorStartupInfo.driverClassPath);

        CommandLine commandLine = new CommandLine(System.getProperty("java.home") + "/bin/java");
        commandLine.addArguments(DEFAULT_JVM_ARGS);
        // TODO allow overriding JVM args

        StringBuilder sb = new StringBuilder();
        Set<String> entries = new HashSet<>();

        for (File cp : classPaths) {
            String entry = cp.getAbsolutePath();
            if (!(entries.contains(entry))) {
                entries.add(entry);
                sb.append(File.pathSeparator);
                sb.append(entry);
            }
        }
        commandLine.addArgument("-classpath");
        commandLine.addArgument(sb.toString());
        commandLine.addArgument("jetbrains.mps.tool.builder.AntBootstrap");
        commandLine.addArgument(generatorStartupInfo.driverClassName);

        Path inputFile;
        try {
            inputFile = Files.createTempFile("mpsinput", ".bin");
            inputFile.toFile().deleteOnExit();
            try (OutputStream stream = Files.newOutputStream(inputFile);
                 ObjectOutputStream oos = new ObjectOutputStream(stream)) {
                oos.writeObject(input);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing generation script to file", e);
        }

        commandLine.addArgument(inputFile.toAbsolutePath().toString());
        return commandLine;
    }

    private static File getToolsJar() {
        String javaHome = System.getProperty("java.home");
        return new File(javaHome, "../lib/tools.jar");
    }

    private static boolean startsWith(String path, String prefix) {
        return path.startsWith(prefix) && (path.length() == prefix.length() || prefix.endsWith(File.separator) || path.charAt(prefix.length()) == File.separatorChar);
    }

    private static Collection<File> calculateMpsClassPath(File mpsHome) throws MojoFailureException {
        Collection<File> result = new LinkedHashSet<>();
        gatherAllJarsUnder(new File(mpsHome, "lib"), result);
        return result;
    }

    private static void gatherAllJarsUnder(File dir, Collection<File> result) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        //  to provide right order of class loading,
        //  files go first
        for (File f : children) {
            if (!(f.isDirectory())) {
                if (f.getName().endsWith(".jar")) {
                    result.add(f);
                }
            }
        }
        for (File f : children) {
            if (f.isDirectory()) {
                gatherAllJarsUnder(f, result);
            }
        }
    }
}
