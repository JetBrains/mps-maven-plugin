package org.jetbrains.mps.mavenplugin.mps;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MpsModule {
    public final List<File> libraries;

    public MpsModule(List<File> libraries) {
        this.libraries = libraries;
    }

    public static MpsModule readFromFile(File root) {
        if (root.isFile()) {
            return new MpsModule(Collections.singletonList(root));
        }

        Collection<File> jarFiles = FileUtils.listFiles(root, new String[] {"jar"}, true);
        return new MpsModule(ImmutableList.copyOf(jarFiles));
    }
}
