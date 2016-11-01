package org.jetbrains.mps.mavenplugin;

import com.google.common.base.Throwables;
import com.google.common.collect.ObjectArrays;
import com.google.common.io.Files;
import jetbrains.mps.tool.common.GeneratorInput;
import jetbrains.mps.tool.common.GeneratorStartupInfo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
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
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generate Java files from MPS models written in BaseLanguage.
 */
@Mojo(name = "generate-java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateJavaMojo extends AbstractMojo {

    private static final String MY_KEY = "org.jetbrains.mps:mps-maven-plugin";
    private static final String DRIVER_GROUP_ID = "org.jetbrains.mps";
    private static final String DRIVER_ARTIFACT_ID = "mps-launchee";

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
     * The coordinates of MPS version to use
     */
    @Parameter(required = true)
    private Dependency mps;

    /**
     * Additional used MPS languages or Java libraries that cannot be found via the mappings.
     */
    @Parameter
    private Dependency[] dependencies = new Dependency[0];

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    public void execute() throws MojoExecutionException, MojoFailureException {
        mavenProject.addCompileSourceRoot(outputDirectory.getPath());
        Map<ArtifactCoordinates, File> resolvedDependencies;
        try {
            resolvedDependencies = resolveDependencies(ObjectArrays.concat(mps, dependencies));
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException("Error resolving dependencies", e);
        }

        File temporaryWorkingDirectory = Files.createTempDir();

        try {
            Map<ArtifactCoordinates, File> extractedDependencies = extractDependencies(resolvedDependencies, temporaryWorkingDirectory);

            // Remove MPS from extracted dependencies so that its jars are not needlessly included into generator input libraries
            File mpsHome = extractedDependencies.remove(toCoordinates(mps));

            Dependency driver = new Dependency();
            driver.setGroupId(DRIVER_GROUP_ID);
            driver.setArtifactId(DRIVER_ARTIFACT_ID);
            driver.setVersion(this.mavenProject.getPlugin(MY_KEY).getVersion());

            DependencyResult dependencyResult = resolveSingleDependencyViaAether(driver);

            logResolutionResult(driver, dependencyResult);

            List<File> driverClassPath = new ArrayList<>();
            for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
                driverClassPath.add(artifactResult.getArtifact().getFile());
            }

            GeneratorStartupInfo startupInfo = new GeneratorStartupInfo(mpsHome,
                    "org.jetbrains.mps.maven.driver.Driver",
                    driverClassPath);

            Mps.launchMps(startupInfo, new GeneratorInput(mavenProject.getBasedir(),
                            modelsDirectory, outputDirectory, getJars(extractedDependencies.values())),
                    getLog());
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, MojoExecutionException.class, MojoFailureException.class);
            throw new MojoExecutionException("Unexpected exception", e);
        } finally {
            tryDeletingDirectory(temporaryWorkingDirectory);
        }
    }

    private void logResolutionResult(Dependency dependency, DependencyResult dependencyResult) {
        Log log = getLog();
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("Dependencies of " + dependency + ":");
        for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
            log.debug("  " + artifactResult.getArtifact());
        }
    }

    private Collection<File> getJars(Collection<File> roots) {
        List<File> jars = new ArrayList<>();

        for (File root : roots) {
            if (root.isFile()) {
                jars.add(root);
            } else if (root.isDirectory()) {
                jars.addAll(FileUtils.listFiles(root, new String[] { "jar" }, true));
            }
        }

        return jars;
    }

    private Map<ArtifactCoordinates, File> extractDependencies(Map<ArtifactCoordinates, File> resolvedDependencies,
                                                               File temporaryWorkingDirectory) throws IOException {
        ZipExtractor extractor = new ZipExtractor(getLog());

        Map<ArtifactCoordinates, File> result = new HashMap<>(resolvedDependencies.size());
        for (Map.Entry<ArtifactCoordinates, File> entry : resolvedDependencies.entrySet()) {
            File extractedRoot;
            extractedRoot = extractDependency(temporaryWorkingDirectory, extractor, entry.getKey(), entry.getValue());
            result.put(entry.getKey(), extractedRoot);
        }

        return result;
    }

    private static File extractDependency(File temporaryWorkingDirectory, ZipExtractor extractor,
                                          ArtifactCoordinates artifactCoordinates, File artifactFile) throws IOException {
        File extractedRoot;
        if (!ArtifactTypes.MODULE_ARCHIVE.equals(artifactCoordinates.extension) && !ArtifactTypes.ZIP.equals(artifactCoordinates.extension)) {
            extractedRoot = artifactFile;
        } else {
            File dir = new File(temporaryWorkingDirectory, artifactFile.getName());
            if (dir.exists()) {
                throw new IOException(String.format("Cannot extract file %s: directory %s already exists",
                        artifactFile, dir));
            }
            extractor.extract(artifactFile, dir);
            extractedRoot = dir;
        }
        return extractedRoot;
    }

    private Map<ArtifactCoordinates, File> resolveDependencies(Dependency[] dependencies) throws DependencyResolutionException {
        // TODO dependency management from Maven project
        // TODO project or plugin repositories?
        DependencyResult result = resolveDependenciesViaAether(dependencies);
        return result.getArtifactResults().stream().collect(Collectors.toMap(
                res -> toCoordinates(res.getArtifact()),
                res -> res.getArtifact().getFile()));
    }

    private DependencyResult resolveDependenciesViaAether(Dependency[] dependencies) throws DependencyResolutionException {
        DependencyRequest request = new DependencyRequest(
                new CollectRequest(toAether(dependencies), null, mavenProject.getRemoteProjectRepositories()),
                null);
        return repoSystem.resolveDependencies(repoSession, request);
    }

    private DependencyResult resolveSingleDependencyViaAether(Dependency root) throws DependencyResolutionException {
        DependencyRequest request = new DependencyRequest(
                new CollectRequest(toAether(root), null, mavenProject.getRemoteProjectRepositories()),
                null);
        return repoSystem.resolveDependencies(repoSession, request);
    }

    private static ArtifactCoordinates toCoordinates(Artifact artifact) {
        return new ArtifactCoordinates(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getExtension(), artifact.getVersion());
    }

    private static ArtifactCoordinates toCoordinates(Dependency dependency) {
        return new ArtifactCoordinates(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                dependency.getType(), dependency.getVersion());
    }

    private static List<org.eclipse.aether.graph.Dependency> toAether(@Nullable Dependency[] dependencies) {
        if (dependencies == null) return Collections.emptyList();

        List<org.eclipse.aether.graph.Dependency> result = new ArrayList<>(dependencies.length);
        for (Dependency dependency : dependencies) {
            result.add(toAether(dependency));
        }
        return result;
    }

    private static org.eclipse.aether.graph.Dependency toAether(Dependency dependency) {
        // Not caring about exclusions for now
        return new org.eclipse.aether.graph.Dependency(
                new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(),
                        dependency.getType(), dependency.getVersion()), dependency.getScope());
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
