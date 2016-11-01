package jetbrains.mps.tool.common;

import java.io.File;
import java.util.Collection;

public class GeneratorStartupInfo {
    public final File mpsHome;
    public final String driverClassName;
    public final Collection<File> driverClassPath;

    public GeneratorStartupInfo(File mpsHome, String driverClassName, Collection<File> driverClassPath) {
        this.mpsHome = mpsHome;
        this.driverClassName = driverClassName;
        this.driverClassPath = driverClassPath;
    }
}
