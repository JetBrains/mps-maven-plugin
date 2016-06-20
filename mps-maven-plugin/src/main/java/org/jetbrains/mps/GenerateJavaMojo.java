package org.jetbrains.mps;

import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.pyx4j.log4j.MavenLogAppender;
import jetbrains.mps.extapi.persistence.FileBasedModelRoot;
import jetbrains.mps.persistence.DefaultModelRoot;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.persistence.SolutionDescriptorPersistence;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.tool.builder.MpsWorker;
import jetbrains.mps.tool.builder.make.GeneratorWorker;
import jetbrains.mps.tool.common.JavaCompilerProperties;
import jetbrains.mps.tool.common.Script;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.vfs.FileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
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
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generate Java files from MPS models written in BaseLanguage.
 */
@Mojo(name = "generate-java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateJavaMojo extends AbstractMojo {
    private static final String ARTIFACT_TYPE_LANGUAGE_ARCHIVE = "lar";

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
     * The version of MPS artifacts to use for generation.
     */
    @Parameter(name = "mpsVersion", property = "mps.version", required = true)
    private String mpsVersion;

    /**
     * Additional used MPS languages or Java libraries.
     */
    @Parameter
    private Dependency[] dependencies;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    // Directory under which the language artifacts are unpacked for the duration of the build.
    // Each artifact's contents are unpacked into a separate subdirectory under this directory.
    private File languagesTempDirectory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try (Closer closer = Closer.create()) {
            MavenLogAppender.startPluginLog(this);
            closer.register(() -> MavenLogAppender.endPluginLog(this));

            languagesTempDirectory = Files.createTempDir();
            closer.register(() -> tryDeletingDirectory(languagesTempDirectory));

            Script script = new Script();
            script.updateLogLevel(Level.TRACE);

            // Disable loading bootstrap libraries, otherwise MPS looks for them in some lib folder under some home path
            // and the plugin doesn't provide anything like that, so it fails.
            script.setLoadBootstrapLibraries(false);

            // Leave compilation to Maven. We can do it since we generate one module at a time, and it lets us (and the
            // plugin user) control the compilation process.
            new JavaCompilerProperties(script).setSkipCompilation(true);

            addLibraryJarsToScript(script, resolveDependencies());

            File file = writeTemporarySolution();
            script.addChunk(Lists.newArrayList(file.getAbsolutePath()), false);

            new GeneratorWorker(script, new MpsWorker.AntLogger() {
                final Log log = getLog();

                @Override
                public void log(String text, Level level) {
                    if (Level.DEBUG.isGreaterOrEqual(level)) {
                        log.debug(text);
                    } else if (level == Level.INFO) {
                        log.info(text);
                    } else if (level == Level.WARN) {
                        log.warn(text);
                    } else {
                        log.error(text);
                    }
                }
            }).work();

            mavenProject.addCompileSourceRoot(outputDirectory.getPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error in mps-maven-plugin", e);
        }
    }

    private void tryDeletingDirectory(File directory) {
        try {
            getLog().debug("Deleting directory " + directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException io) {
            getLog().error("Could not delete directory " + directory, io);
        }
    }

    private File writeTemporarySolution() {
        File file = new File(languagesTempDirectory, "solution.msd");
        SolutionDescriptor descriptor = new SolutionDescriptor();
        descriptor.getModelRootDescriptors().add(newModelRootDescriptor(modelsDirectory));
        descriptor.setOutputPath(outputDirectory.getAbsolutePath());
        descriptor.setNamespace(mavenProject.getGroupId() + "." + mavenProject.getArtifactId());
        descriptor.setId(ModuleId.foreign(mavenProject.getGroupId() + "." + mavenProject.getArtifactId()));
        descriptor.setCompileInMPS(false);

        SolutionDescriptorPersistence.saveSolutionDescriptor(FileSystem.getInstance().getFileByPath(file.getAbsolutePath()),
                descriptor, new MacroHelper() {
                    @Override
                    public String expandPath(@Nullable String path) {
                        return path;
                    }

                    @Override
                    public String shrinkPath(@Nullable String absolutePath) {
                        return absolutePath;
                    }
                });

        file.deleteOnExit();
        return file;
    }

    @NotNull
    private static ModelRootDescriptor newModelRootDescriptor(File modelsDirectory) {
        DefaultModelRoot modelRoot = new DefaultModelRoot();
        modelRoot.setContentRoot(modelsDirectory.getAbsolutePath());
        modelRoot.addFile(FileBasedModelRoot.SOURCE_ROOTS, modelsDirectory.getAbsolutePath());

        ModelRootDescriptor result = new ModelRootDescriptor();
        modelRoot.save(result.getMemento());
        return result;
    }

    private void addLibraryJarsToScript(Script script, List<ArtifactResult> artifactResults) throws IOException {
        for (ArtifactResult artifactResult : artifactResults) {
            Artifact artifact = artifactResult.getArtifact();

            if (ARTIFACT_TYPE_LANGUAGE_ARCHIVE.equals(artifact.getExtension())) {
                File tempDirectory = new File(languagesTempDirectory, artifact.getArtifactId());
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Extracting " + artifact + " to " + tempDirectory);
                }
                List<File> extractedFiles = Lists.newArrayList();
                try (ZipFile zipFile = new ZipFile(artifact.getFile(), ZipFile.OPEN_READ)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.isDirectory()) continue;
                        if (!entry.getName().endsWith(".jar")) continue;

                        getLog().debug("Extracting " + entry.getName());
                        extractedFiles.add(extract(zipFile, entry, tempDirectory));
                    }
                }

                getLog().debug("Extracted " + extractedFiles.size() + " files successfully");

                for (File file : extractedFiles) {
                    script.addLibraryJar(file.getAbsolutePath());
                }
            } else {
                script.addLibraryJar(artifact.getFile().getAbsolutePath());
            }
        }
    }

    /**
     * @return extracted file
     */
    private File extract(ZipFile zipFile, ZipEntry entry, File destinationDirectory) throws IOException {
        File destFile = new File(destinationDirectory, entry.getName());

        try (InputStream input = zipFile.getInputStream(entry);
             FileOutputStream output = FileUtils.openOutputStream(destFile)) {
            IOUtils.copy(input, output);
            return destFile;
        }
    }

    private List<ArtifactResult> resolveDependencies() throws MojoExecutionException {
        List<ArtifactResult> artifactResults;
        try {
            artifactResults = repoSystem.resolveArtifacts(repoSession, toArtifactRequests(
                    getDefaultDependencies(mpsVersion), dependencies, mavenProject.getRemotePluginRepositories()));
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Failure resolving artifacts", e);
        }
        return artifactResults;
    }

    @NotNull
    private static Collection<ArtifactRequest> toArtifactRequests(
            Artifact[] defaultDependencies, Dependency[] dependencies, List<RemoteRepository> repositories) {
        List<ArtifactRequest> requests = Lists.newArrayList();

        for (Artifact art : defaultDependencies) {
            requests.add(new ArtifactRequest().setArtifact(art).setRepositories(repositories));
        }

        if (dependencies != null) {
            for (Dependency dep : dependencies) {
                requests.add(new ArtifactRequest()
                        .setArtifact(toArtifact(dep))
                        .setRepositories(repositories));
            }
        }
        return requests;
    }

    @NotNull
    private static DefaultArtifact toArtifact(Dependency dep) {
        return new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion());
    }

    private static Artifact[] getDefaultDependencies(String mpsVersion) {
        return new Artifact[]{
                new DefaultArtifact("org.jetbrains.mps.languages", "languageDesign", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps.languages", "baseLanguage", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps.languages", "make", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps", "mps-lib", "lar", mpsVersion)
        };
    }
}
