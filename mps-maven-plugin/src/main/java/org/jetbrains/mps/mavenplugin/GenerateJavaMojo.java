package org.jetbrains.mps.mavenplugin;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import jetbrains.mps.library.ModulesMiner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.mavenplugin.mps.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate Java files from MPS models written in BaseLanguage.
 */
@Mojo(name = "generate-java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateJavaMojo extends AbstractMojo {

    /**
     * Input directory containing MPS models
     */
    @Parameter(defaultValue = "${basedir}/src/main/mps", required = true)
    private File modelsDirectory;
    /**
     * Output directory for the generated files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/mps", required = true)
    private File outputDirectory;

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject mavenProject;

    /**
     * Additional used MPS languages or Java libraries that cannot be found via the mappings.
     */
    @Parameter
    private Dependency[] dependencies;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    public void execute() throws MojoExecutionException, MojoFailureException {
        mavenProject.addCompileSourceRoot(outputDirectory.getPath());
        Map<ArtifactCoordinates, File> resolvedDependencies;
        try {
            resolvedDependencies = resolveDependencies(dependencies);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException("Error resolving dependencies", e);
        }

        File temporaryWorkingDirectory = Files.createTempDir();

        try {
            Collection<File> extractedDependencies = extractDependencies(resolvedDependencies, temporaryWorkingDirectory);
            launchMps(extractedDependencies, temporaryWorkingDirectory);
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, MojoExecutionException.class, MojoFailureException.class);
            throw new MojoExecutionException("Unexpected exception", e);
        } finally {
            tryDeletingDirectory(temporaryWorkingDirectory);
        }
    }

    private void launchMps(Iterable<File> extractedDependencies, File temporaryWorkingDirectory) throws MojoExecutionException {
        MavenLogAppender.startPluginLog(this);
        try {
            Collection<MpsModule> modules = readModules(extractedDependencies);

            List<File> libraries = getLibraries(modules);

            TemporarySolution temporarySolution = new TemporarySolution(
                    modelsDirectory, outputDirectory, "mpsmavensolution");

            File solutionFile = new File(temporaryWorkingDirectory, "solution.msd");
            temporarySolution.writeToFile(solutionFile);

            GeneratorInput generatorInput = new GeneratorInput(solutionFile, libraries);
            Mps.runGenerator(getLog(), generatorInput);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            MavenLogAppender.endPluginLog(this);
        }
    }

    private Collection<MpsModule> readModules(Iterable<File> extractedDependencies) {
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

    private Collection<File> extractDependencies(Map<ArtifactCoordinates, File> resolvedDependencies,
                                                 File temporaryWorkingDirectory) throws IOException {
        ZipExtractor extractor = new ZipExtractor(getLog());

        List<File> result = new ArrayList<>(resolvedDependencies.size());
        for (Map.Entry<ArtifactCoordinates, File> entry : resolvedDependencies.entrySet()) {
            if (!ArtifactTypes.MODULE_ARCHIVE.equals(entry.getKey().extension)) {
                result.add(entry.getValue());
                continue;
            }

            File dir = new File(temporaryWorkingDirectory, entry.getValue().getName());
            if (dir.exists()) {
                throw new IOException(String.format("Cannot extract file %s: directory %s already exists",
                        entry.getValue(), dir));
            }
            extractor.extract(entry.getValue(), dir);
            result.add(dir);
        }

        return result;
    }

    private Map<ArtifactCoordinates, File> resolveDependencies(Dependency[] dependencies) throws DependencyResolutionException {
        // TODO dependency management from Maven project
        // TODO project or plugin repositories?
        DependencyRequest request = new DependencyRequest(
                new CollectRequest(toAether(dependencies), null, mavenProject.getRemoteProjectRepositories()),
                null);
        DependencyResult result = repoSystem.resolveDependencies(repoSession, request);
        return result.getArtifactResults().stream().collect(Collectors.toMap(
                res -> toCoordinates(res.getArtifact()),
                res -> res.getArtifact().getFile()));
    }

    private static ArtifactCoordinates toCoordinates(Artifact artifact) {
        return new ArtifactCoordinates(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getExtension(), artifact.getVersion());
    }

    private static List<org.eclipse.aether.graph.Dependency> toAether(@Nullable Dependency[] dependencies) {
        if (dependencies == null) return Collections.emptyList();

        List<org.eclipse.aether.graph.Dependency> result = new ArrayList<>(dependencies.length);
        for (Dependency dependency : dependencies) {
            // Not caring about exclusions for now
            result.add(new org.eclipse.aether.graph.Dependency(
                    new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                            dependency.getType(), dependency.getVersion()), dependency.getScope()));
        }
        return result;
    }

    private void tryDeletingDirectory(File directory) {
        try {
            getLog().debug("Deleting directory " + directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException io) {
            getLog().error("Could not delete directory " + directory, io);
        }
    }
}
