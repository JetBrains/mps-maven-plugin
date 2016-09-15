package org.jetbrains.mps;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipExtractor {
    private final Log log;

    ZipExtractor(Log log) {
        this.log = log;
    }

    void extract(File archive, File toDirectory) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Extracting " + archive + " to " + toDirectory);
        }
        int count = 0;
        try (ZipFile zipFile = new ZipFile(archive, ZipFile.OPEN_READ)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                log.debug("Extracting " + entry.getName());
                extract(zipFile, entry, toDirectory);
                count++;
            }
        }

        log.debug("Extracted " + count + " files successfully");
    }

    /**
     * @return extracted file
     */
    private static File extract(ZipFile zipFile, ZipEntry entry, File destinationDirectory) throws IOException {
        File destFile = new File(destinationDirectory, entry.getName());

        try (InputStream input = zipFile.getInputStream(entry);
             FileOutputStream output = FileUtils.openOutputStream(destFile)) {
            IOUtils.copy(input, output);
            return destFile;
        }
    }
}
