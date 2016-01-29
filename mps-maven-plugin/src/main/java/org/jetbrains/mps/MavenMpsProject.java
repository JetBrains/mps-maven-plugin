package org.jetbrains.mps;

import jetbrains.mps.project.ProjectBase;
import jetbrains.mps.project.structure.project.ProjectDescriptor;
import org.jetbrains.annotations.NotNull;

public class MavenMpsProject extends ProjectBase {

    private final String name;

    public MavenMpsProject(String name) {
        super(new ProjectDescriptor(name));
        this.name = name;
    }

    @Override
    public void save() {
    }

    @Override
    public <T> T getComponent(Class<T> t) {
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
}
