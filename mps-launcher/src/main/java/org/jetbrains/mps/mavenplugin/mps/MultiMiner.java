package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.persistence.PersistenceRegistry;
import jetbrains.mps.persistence.java.library.JavaClassesPersistence;
import jetbrains.mps.vfs.IFile;
import jetbrains.mps.vfs.impl.IoFile;

import java.io.File;
import java.util.Collection;

/**
 * Multiple-use interface to {@link jetbrains.mps.library.ModulesMiner}.
 */
public class MultiMiner implements AutoCloseable {
    private final ComponentTracker componentTracker = new ComponentTracker();

    public MultiMiner() {
        PersistenceRegistry persistenceRegistry = componentTracker.init(new PersistenceRegistry());
        componentTracker.init(new JavaClassesPersistence(persistenceRegistry));
    }

    public Collection<ModulesMiner.ModuleHandle> collectModules(File file) {
        return new ModulesMiner().collectModules(new IoFile(file.getAbsolutePath())).getCollectedModules();
    }

    @Override
    public void close() {
        componentTracker.close();
    }
}
