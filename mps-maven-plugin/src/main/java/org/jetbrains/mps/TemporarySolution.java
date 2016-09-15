package org.jetbrains.mps;

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

class TemporarySolution {
    private final File modelsDirectory;
    private final File outputDirectory;
    private final String namespace;

    TemporarySolution(File modelsDirectory, File outputDirectory, String namespace) {
        this.modelsDirectory = modelsDirectory;
        this.outputDirectory = outputDirectory;
        this.namespace = namespace;
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

    private static SolutionDescriptor toSolutionDescriptor(TemporarySolution solution) {
        SolutionDescriptor descriptor = new SolutionDescriptor();
        descriptor.getModelRootDescriptors().add(newModelRootDescriptor(solution.modelsDirectory));
        descriptor.setOutputPath(solution.outputDirectory.getAbsolutePath());
        descriptor.setNamespace(solution.namespace);
        descriptor.setId(ModuleId.foreign(solution.namespace));
        descriptor.setCompileInMPS(false);
        return descriptor;
    }

    void writeToFile(File solutionFile) {
        jetbrains.mps.project.persistence.SolutionDescriptorPersistence.saveSolutionDescriptor(new IoFile(solutionFile.getAbsolutePath()),
                toSolutionDescriptor(this), new MacroHelper() {
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
