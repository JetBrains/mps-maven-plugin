package org.jetbrains.mps;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.pyx4j.log4j.MavenLogAppender;
import jetbrains.mps.extapi.model.SModelBase;
import jetbrains.mps.extapi.module.SModuleBase;
import jetbrains.mps.extapi.persistence.FileDataSource;
import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.structure.modules.ModuleReference;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.smodel.ModuleRepositoryFacade;
import jetbrains.mps.smodel.adapter.structure.language.SLanguageAdapterById;
import jetbrains.mps.vfs.FileSystem;
import jetbrains.mps.vfs.FileSystemProvider;
import jetbrains.mps.vfs.IFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.persistence.PersistenceFacade;
import org.jetbrains.mps.tool.builder.GeneratorWorker;
import org.jetbrains.mps.tool.builder.Script;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generate Java files from MPS models written in BaseLanguage.
 */
@Mojo(name = "generate-java", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true)
public class GenerateJavaMojo extends AbstractMojo {
    public static final String ARTIFACT_TYPE_LANGUAGE_ARCHIVE = "lar";
    private final FileSystemProvider fileSystemProvider = FileSystem.getInstance().getFileSystemProvider();

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
        MavenLogAppender.startPluginLog(this);
        Script script = new Script();
        script.setLoadBootstrapLibraries(false);

        for (File file : ModelFiles.listModelFiles(modelsDirectory)) {
            script.addModelFile(file);
        }

        try {
            languagesTempDirectory = Files.createTempDir();
            addLibraryJarsToScript(script, resolveDependencies());

            GeneratorWorker worker = new GeneratorWorker(script, getLog());
            worker.setupEnvironment();

            final MavenMpsProject project = new MavenMpsProject(mavenProject.getName());
            project.getRepository().getModelAccess().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<SModelBase> models = Lists.newArrayList();
                        for (File file : ModelFiles.listModelFiles(modelsDirectory)) {
                            models.add((SModelBase) loadModel(file));
                        }

                        createModule(models);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                private void createModule(List<SModelBase> models) {
                    SModule module = ModuleRepositoryFacade.createModule(new ModulesMiner.ModuleHandle(fileSystemProvider.getFile(mavenProject.getFile().getAbsolutePath()),
                            createSolutionDescriptor(models)), project);
                    for (SModelBase model : models) {
                        ((SModuleBase) module).registerModel(model);
                    }
                }
            });

            worker.workFromBaseGenerator();

            mavenProject.addCompileSourceRoot(outputDirectory.getPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error in mps-maven-plugin", e);
        } finally {
            try {
                getLog().debug("Deleting directory " + languagesTempDirectory);
                FileUtils.deleteDirectory(languagesTempDirectory);
            } catch (IOException io) {
                getLog().error("Could not delete temporary directory " + languagesTempDirectory, io);
            }
            MavenLogAppender.endPluginLog(this);
        }
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
                ZipFile zipFile = new ZipFile(artifact.getFile(), ZipFile.OPEN_READ);
                try {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.isDirectory()) continue;
                        if (!entry.getName().endsWith(".jar")) continue;

                        getLog().debug("Extracting " + entry.getName());
                        extractedFiles.add(extract(zipFile, entry, tempDirectory));
                    }
                } finally {
                    zipFile.close();
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
    private File extract(ZipFile zipFile, ZipEntry entry, File destDirectory) throws IOException {
        InputStream input = null;
        FileOutputStream output = null;
        boolean threw = true;
        try {
            input = zipFile.getInputStream(entry);
            File destFile = new File(destDirectory, entry.getName());
            output = FileUtils.openOutputStream(destFile);

            IOUtils.copy(input, output);

            threw = false;
            return destFile;
        } finally {
            Closeables.closeQuietly(input);
            Closeables.close(output, threw);
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

    private org.jetbrains.mps.openapi.model.SModel loadModel(File file) throws IOException {
        final IFile ifile = FileSystem.getInstance().getFileByPath(file.getAbsolutePath());
        return PersistenceFacade.getInstance()
                .getModelFactory(FilenameUtils.getExtension(ifile.getName()))
                .load(new FileDataSource(ifile), new HashMap<String, String>());
    }

    /**
     * Creates a synthetic solution descriptor for a collection of models since MPS generator works on solutions, not
     * bare models.
     * @param models the models to include in the solution.
     * @return a synthetic solution descriptor with a randomly generated ID and namespace set to 'internal'.
     */
    @NotNull
    private SolutionDescriptor createSolutionDescriptor(List<SModelBase> models) {
        SolutionDescriptor descriptor = new SolutionDescriptor();
        descriptor.setId(ModuleId.regular());
        descriptor.setNamespace("internal");

        descriptor.setOutputPath(outputDirectory.getPath());

        for (SModelBase model : models) {
            Collection<SLanguage> usedLanguages = model.getSModel().usedLanguages();

            for (SLanguage lang : usedLanguages) {
                SLanguageAdapterById langById = (SLanguageAdapterById) lang;

                descriptor.getUsedLanguages().add(
                        new ModuleReference(lang.getQualifiedName(), ModuleId.regular(langById.getId().getIdValue())));
            }
        }
        return descriptor;
    }

    private static Artifact[] getDefaultDependencies(String mpsVersion) {
        return new Artifact[] {
                new DefaultArtifact("org.jetbrains.mps.languages", "languageDesign", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps.languages", "baseLanguage", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps.languages", "make", "lar", mpsVersion),
                new DefaultArtifact("org.jetbrains.mps", "mps-lib", "lar", mpsVersion)
        };
    }
}
