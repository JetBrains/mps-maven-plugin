package org.jetbrains.mps;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

public class ModelFiles {
    @NotNull
    public static Collection<File> listModelFiles(File modelsDirectory) {
        return FileUtils.listFiles(modelsDirectory, null, true);
    }
}
