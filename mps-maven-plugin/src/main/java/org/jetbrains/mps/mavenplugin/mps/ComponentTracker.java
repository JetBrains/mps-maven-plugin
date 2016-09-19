package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.components.CoreComponent;

import java.util.ArrayDeque;
import java.util.Deque;

class ComponentTracker implements AutoCloseable {
    private final Deque<CoreComponent> components = new ArrayDeque<>();

    <T extends CoreComponent> T init(T component) {
        component.init();
        components.push(component);
        return component;
    }

    @Override
    public void close() {
        while (!components.isEmpty()) {
            components.pop().dispose();
        }
    }
}
