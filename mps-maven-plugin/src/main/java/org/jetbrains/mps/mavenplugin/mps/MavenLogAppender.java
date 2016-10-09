package org.jetbrains.mps.mavenplugin.mps;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

public class MavenLogAppender extends AppenderSkeleton {
	
	private static Log mavenLog;

	public static void startPluginLog(AbstractMojo mojo) {
		mavenLog = mojo.getLog();
	}

	public static void endPluginLog(AbstractMojo mojo) {
		mavenLog = null;
	}
	
	protected void append(LoggingEvent event) {
		if (mavenLog == null) {
			return;
		}
		Level level = event.getLevel();
		if (Level.DEBUG.equals(level) && !(mavenLog.isDebugEnabled())) {
			return;
		}

		String text = this.layout.format(event);
		Throwable throwable = null;
		if (event.getThrowableInformation() != null) {
			throwable = event.getThrowableInformation().getThrowable();
		}
		
		if (Level.DEBUG.equals(level) || Level.TRACE.equals(level)) {
			mavenLog.debug(text, throwable);
		} else if (Level.INFO.equals(level)) {
			mavenLog.info(text, throwable);
		} else if (Level.WARN.equals(level)) {
			mavenLog.warn(text, throwable);
		} else if (Level.ERROR.equals(level) || Level.FATAL.equals(level)) {
			mavenLog.error(text, throwable);
		} else {
			mavenLog.error(text, throwable);
		}
	}

	public void close() {
		mavenLog = null;
	}

	public boolean requiresLayout() {
		return true;
	}

	
	
	
}
