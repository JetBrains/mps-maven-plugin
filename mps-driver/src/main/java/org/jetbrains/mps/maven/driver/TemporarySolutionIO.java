package org.jetbrains.mps.maven.driver;

import jetbrains.mps.extapi.persistence.FileBasedModelRoot;
import jetbrains.mps.persistence.DefaultModelRoot;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.vfs.impl.IoFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public class TemporarySolutionIO {
    @NotNull
    private static ModelRootDescriptor newModelRootDescriptor(File modelsDirectory) {
        DefaultModelRoot modelRoot = new DefaultModelRoot();
        modelRoot.setContentRoot(modelsDirectory.getAbsolutePath());
        modelRoot.addFile(FileBasedModelRoot.SOURCE_ROOTS, modelsDirectory.getAbsolutePath());

        ModelRootDescriptor result = new ModelRootDescriptor();
        modelRoot.save(result.getMemento());
        return result;
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

    static void writeToFile(TemporarySolution temporarySolution, Path solutionFile) {
        jetbrains.mps.project.persistence.SolutionDescriptorPersistence.saveSolutionDescriptor(
                new IoFile(solutionFile.toAbsolutePath().toString()),
                toSolutionDescriptor(temporarySolution), new MacroHelper() {
                    @Override
                    public String expandPath(@Nullable String path) {
                        return path;
                    }

                    @Override
                    public String shrinkPath(@Nullable String absolutePath) {
                        return absolutePath;
                    }
                });
    }
}
