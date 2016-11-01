package org.jetbrains.mps.mavenplugin;

import java.io.File;
import java.util.Collection;

class GeneratorStartupInfo {
    final File mpsHome;
    final String driverClassName;
    final Collection<File> driverClassPath;

    GeneratorStartupInfo(File mpsHome, String driverClassName, Collection<File> driverClassPath) {
        this.mpsHome = mpsHome;
        this.driverClassName = driverClassName;
        this.driverClassPath = driverClassPath;
    }
}
