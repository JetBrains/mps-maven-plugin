package org.jetbrains.mps.maven.driver;

import jetbrains.mps.extapi.persistence.FileBasedModelRoot;
import jetbrains.mps.persistence.DefaultModelRoot;
import jetbrains.mps.project.ModuleId;
import jetbrains.mps.project.persistence.ModuleDescriptorPersistence;
import jetbrains.mps.project.persistence.SolutionDescriptorPersistence;
import jetbrains.mps.project.structure.model.ModelRootDescriptor;
import jetbrains.mps.project.structure.modules.SolutionDescriptor;
import jetbrains.mps.util.JDOMUtil;
import jetbrains.mps.util.MacroHelper;
import jetbrains.mps.vfs.impl.IoFile;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

class TemporarySolutionIO {
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

    static void writeToFile(TemporarySolution temporarySolution, Path solutionFile) throws IOException {
        SolutionDescriptor descriptor = toSolutionDescriptor(temporarySolution);
        org.jdom.Element save = new SolutionDescriptorPersistence(new MacroHelper.MacroNoHelper()).save(
                descriptor);
        IoFile ioFile = new IoFile(solutionFile.toAbsolutePath().toString());
        try (OutputStream os = ioFile.openOutputStream()){
            JDOMUtil.writeDocument(new Document(save), os);
        }

        ModuleDescriptorPersistence.setTimestamp(descriptor, ioFile);
    }
}
