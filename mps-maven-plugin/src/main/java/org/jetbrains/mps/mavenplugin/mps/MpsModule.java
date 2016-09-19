package org.jetbrains.mps.mavenplugin.mps;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.mps.openapi.module.SModuleReference;

import java.io.File;
import java.util.List;
import java.util.Set;

public class MpsModule {
    public final Set<SModuleReference> ids;
    // Maps a module to modules that depend on it
    public final Multimap<SModuleReference, SModuleReference> dependencyIds;
    public final List<File> libraries;

    MpsModule(@NotNull Set<SModuleReference> ids, @NotNull Multimap<SModuleReference, SModuleReference> dependencyIds,
              @NotNull List<File> libraries) {
        this.ids = ids;
        this.dependencyIds = dependencyIds;
        this.libraries = libraries;
    }

}
