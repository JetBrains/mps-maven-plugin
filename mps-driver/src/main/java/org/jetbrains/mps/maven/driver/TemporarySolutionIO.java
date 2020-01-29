package org.jetbrains.mps.maven.driver;

import jetbrains.mps.persistence.DefaultModelRoot;
import jetbrains.mps.persistence.MementoImpl;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.io.DescriptorIO;
import jetbrains.mps.project.io.DescriptorIOException;
import jetbrains.mps.project.io.DescriptorIOFacade;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.vfs.*;
import jetbrains.mps.vfs.impl.IoFileSystem;
import jetbrains.mps.vfs.iofs.file.LocalIoFileSystem;
import jetbrains.mps.vfs.iofs.jar.JarIoFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.persistence.Memento;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

class TemporarySolutionIO {
    @NotNull
    private static ModelRootDescriptor newModelRootDescriptor(File modelsDirectory) {
        IFileSystem fs = getFS();
        return DefaultModelRoot.createDescriptor(
                fs.getFile(modelsDirectory.getAbsolutePath()),
                fs.getFile(modelsDirectory.getAbsolutePath())
        );
    }

    private static SolutionDescriptor toSolutionDescriptor(TemporarySolution solution) {
        SolutionDescriptor descriptor = new SolutionDescriptor();

        Collection<ModelRootDescriptor> modelRoots = descriptor.getModelRootDescriptors();
        modelRoots.add(newModelRootDescriptor(solution.modelsDirectory));
        for (File javaSourceDir : solution.javaSourceDirs) {
            modelRoots.add(javaSourceStubMRDescriptor(javaSourceDir));
        }

        descriptor.setOutputPath(solution.outputDirectory.getAbsolutePath());
        descriptor.setNamespace(solution.moduleName);
        descriptor.setId(ModuleId.foreign(solution.moduleName));
        descriptor.setCompileInMPS(false);

        return descriptor;
    }

    private static SolutionDescriptor toStubSolutionDescriptor(String namespace, File jarFile) {
        SolutionDescriptor descriptor = new SolutionDescriptor();
        descriptor.getModelRootDescriptors().add(ModelRootDescriptor.addJavaStubModelRoot(jarFile, Collections.emptyList()));
        descriptor.setNamespace(namespace);
        descriptor.setId(ModuleId.foreign(namespace));
        descriptor.setCompileInMPS(false);
        return descriptor;
    }

    // FIXME depends on inner workings of JavaSourceModelRoot
    private static ModelRootDescriptor javaSourceStubMRDescriptor(File file) {
        Memento m = new MementoImpl();
        m.put(PATH_KEY, file.getAbsolutePath());
        return new ModelRootDescriptor(JAVA_SOURCE_STUBS_TYPE, m);
    }

    static void writeToFile(TemporarySolution temporarySolution, Path solutionFile) throws DescriptorIOException {
        IFileSystem fs = getFS();
        DescriptorIO<SolutionDescriptor> io =
                new DescriptorIOFacade(macroHelpers()).standardProvider().solutionDescriptorIO();
        io.writeToFile(
                toSolutionDescriptor(temporarySolution),
                fs.getFile(solutionFile.toAbsolutePath().toString())
        );
    }

    static void createStubSolution(File descriptorFile, String moduleName, File jarFile) throws DescriptorIOException {
        IFileSystem fs = getFS();

        IFile f = fs.getFile(descriptorFile);
        SolutionDescriptor d = toStubSolutionDescriptor(moduleName, jarFile);

        DescriptorIO<SolutionDescriptor> io =
                new DescriptorIOFacade(macroHelpers()).standardProvider().solutionDescriptorIO();
        io.writeToFile(d, f);
    }

    private static MacroHelper.Source macroHelpers() {
        return new MacroHelper.Source() {
            @NotNull
            @Override
            public MacroHelper global() {
                return AsIs.INSTANCE;
            }

            @NotNull
            @Override
            public MacroHelper module(SModule m) {
                return AsIs.INSTANCE;
            }

            @NotNull
            @Override
            public MacroHelper moduleFile(IFile f) {
                return AsIs.INSTANCE;
            }

            @NotNull
            @Override
            public MacroHelper projectFile(IFile f) {
                return AsIs.INSTANCE;
            }
        };
    }

    private static class AsIs implements MacroHelper {
        static AsIs INSTANCE = new AsIs();

        @Override
        public String expandPath(@Nullable String path) {
            return path;
        }

        @Override
        public String shrinkPath(@Nullable String absolutePath) {
            return absolutePath;
        }
    }

    // FIXME find a proper way to init fs
    private static IFileSystem getFS() {
        if (fs == null) {
            VFSManager mgr = new VFSManager();
            fs = new LocalIoFileSystem(mgr);
            IFileSystem jarFs = new JarIoFileSystem(mgr);
            mgr.registerFS(VFSManager.JAVA_IO_FILE_FS, fs);
            mgr.registerFS(VFSManager.JAVA_IO_JAR_FS, jarFs);
            // Files.fromURL() depends on the following
            IoFileSystem.newInstance(mgr);
            FileSystem obsoleteFs = IoFileSystem.INSTANCE;
            FileSystemExtPoint.setFS(obsoleteFs);
        }
        return fs;
    }

    private static IFileSystem fs;

    private static final String PATH_KEY = "path";
    public static final String JAVA_SOURCE_STUBS_TYPE = "java_source_stubs";
}
