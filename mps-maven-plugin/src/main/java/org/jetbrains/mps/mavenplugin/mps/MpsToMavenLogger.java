package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.tool.builder.MpsWorker;
import org.apache.log4j.Level;
import org.apache.maven.plugin.logging.Log;

class MpsToMavenLogger implements MpsWorker.AntLogger {
    private final Log log;

    MpsToMavenLogger(Log log) {
        this.log = log;
    }

    @Override
    public void log(String text, Level level) {
        if (Level.DEBUG.isGreaterOrEqual(level)) {
            log.debug(text);
        } else if (level == Level.INFO) {
            log.info(text);
        } else if (level == Level.WARN) {
            log.warn(text);
        } else {
            log.error(text);
        }
    }
}
