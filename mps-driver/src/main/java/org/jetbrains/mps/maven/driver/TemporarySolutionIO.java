package org.jetbrains.mps.maven.driver;

import jetbrains.mps.persistence.DefaultModelRoot;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.io.DescriptorIO;
import jetbrains.mps.project.io.DescriptorIOException;
import jetbrains.mps.project.io.DescriptorIOFacade;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.vfs.FileSystem;
import jetbrains.mps.vfs.IFile;
import jetbrains.mps.vfs.impl.IoFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.module.SModule;

import java.io.File;
import java.nio.file.Path;

class TemporarySolutionIO {
    @NotNull
    private static ModelRootDescriptor newModelRootDescriptor(File modelsDirectory) {
        FileSystem fs = IoFileSystem.INSTANCE;
        return DefaultModelRoot.createDescriptor(
                fs.getFile(modelsDirectory.getAbsolutePath()),
                fs.getFile(modelsDirectory.getAbsolutePath())
        );
    }

    private static SolutionDescriptor toSolutionDescriptor(TemporarySolution solution) {
        SolutionDescriptor descriptor = new SolutionDescriptor();
        descriptor.getModelRootDescriptors().add(newModelRootDescriptor(solution.modelsDirectory));
        descriptor.setOutputPath(solution.outputDirectory.getAbsolutePath());
        descriptor.setNamespace("mpsmaventemp");
        descriptor.setId(ModuleId.foreign("mpsmaventemp"));
        descriptor.setCompileInMPS(false);
        return descriptor;
    }

    static void writeToFile(TemporarySolution temporarySolution, Path solutionFile) throws DescriptorIOException {
        FileSystem fs = IoFileSystem.INSTANCE;
        DescriptorIO<SolutionDescriptor> io =
                new DescriptorIOFacade(macroHelpers()).standardProvider().solutionDescriptorIO();
        io.writeToFile(
                toSolutionDescriptor(temporarySolution),
                fs.getFile(solutionFile.toAbsolutePath().toString())
        );
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
}
