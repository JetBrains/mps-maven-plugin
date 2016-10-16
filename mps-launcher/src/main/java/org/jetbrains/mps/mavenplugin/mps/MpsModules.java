package org.jetbrains.mps.mavenplugin.mps;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.project.structure.modules.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.mps.openapi.language.SLanguage;
import org.jetbrains.mps.openapi.module.SDependencyScope;
import org.jetbrains.mps.openapi.module.SModuleReference;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MpsModules {

    public static MpsModule fromRootAndModuleHandles(File root, Collection<ModulesMiner.ModuleHandle> moduleHandles) {
        return new MpsModule(getModuleIds(moduleHandles), getDependencyIds(moduleHandles), getLibraries(root));
    }

    static Multimap<SModuleReference, SModuleReference> getDependencyIds(Iterable<ModulesMiner.ModuleHandle> moduleHandles) {
        Multimap<SModuleReference, SModuleReference> result = HashMultimap.create();
        for (ModulesMiner.ModuleHandle handle : moduleHandles) {
            ModuleDescriptor descriptor = handle.getDescriptor();
            if (descriptor == null) {
                continue;
            }
            addDependencies(result, descriptor);

            DeploymentDescriptor deploymentDescriptor = descriptor.getDeploymentDescriptor();
            if (deploymentDescriptor != null) {
                addDependencies(result, deploymentDescriptor);
            }

            if (descriptor instanceof LanguageDescriptor) {
                LanguageDescriptor languageDescriptor = (LanguageDescriptor) descriptor;
                for (GeneratorDescriptor generatorDescriptor : languageDescriptor.getGenerators()) {
                    addDependencies(result, generatorDescriptor);
                }

                SModuleReference myModuleReference = descriptor.getModuleReference();
                for (SModuleReference extendedLanguage : languageDescriptor.getExtendedLanguages()) {
                    result.put(extendedLanguage, myModuleReference);
                }
            }
        }
        return result;
    }

    private static void addDependencies(Multimap<SModuleReference, SModuleReference> result, ModuleDescriptor descriptor) {
        for (Dependency dependency : descriptor.getDependencies()) {
            if (dependency.getScope() == SDependencyScope.DESIGN) {
                continue;
            }
            result.put(dependency.getModuleRef(), descriptor.getModuleReference());
        }

        if (descriptor instanceof GeneratorDescriptor) {
            // Languages used by generators are required during generation
            for (SLanguage language : descriptor.getLanguageVersions().keySet()) {
                result.put(language.getSourceModuleReference(), descriptor.getModuleReference());
            }
        }
    }

    private static Set<SModuleReference> getModuleIds(Iterable<ModulesMiner.ModuleHandle> moduleHandles) {
        ImmutableSet.Builder<SModuleReference> idsBuilder = ImmutableSet.builder();

        for (ModulesMiner.ModuleHandle moduleHandle : moduleHandles) {
            ModuleDescriptor descriptor = moduleHandle.getDescriptor();
            if (descriptor == null) {
                continue;
            }

            idsBuilder.add(descriptor.getModuleReference());

            if (descriptor instanceof LanguageDescriptor) {
                for (GeneratorDescriptor generatorDescriptor : ((LanguageDescriptor) descriptor).getGenerators()) {
                    idsBuilder.add(generatorDescriptor.getModuleReference());
                }
            }
        }
        return idsBuilder.build();
    }

    private static List<File> getLibraries(File root) {
        List<File> libraries;
        if (root.isFile()) {
            libraries = ImmutableList.of(root);
        } else {
            Collection<File> jarFiles = FileUtils.listFiles(root, new String[]{"jar"}, true);
            libraries = ImmutableList.copyOf(jarFiles);
        }
        return libraries;
    }
}
