package org.jetbrains.mps.mavenplugin;

import com.google.common.io.Files;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.mps.mavenplugin.ZipExtractor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ZipExtractorTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsDirectoryIfZipHasAtLeastOneEntry() throws IOException {
        File jarFile = temporaryFolder.newFile("input.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(jarFile))) {
            stream.putNextEntry(new ZipEntry("irrelevant"));
        }

        File extractTo = new File(temporaryFolder.getRoot(), "output");
        new ZipExtractor(mock(Log.class)).extract(jarFile, extractTo);

        assertTrue("should have created directory", extractTo.isDirectory());
    }

    @Test
    public void canExtractFile() throws IOException {
        File jarFile = temporaryFolder.newFile("input.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(jarFile))) {
            stream.putNextEntry(new ZipEntry("somefile.txt"));
            stream.write("Hello, world!".getBytes());
        }

        File extractTo = new File(temporaryFolder.getRoot(), "output");
        new ZipExtractor(mock(Log.class)).extract(jarFile, extractTo);

        File somefileTxt = new File(extractTo, "somefile.txt");
        assertEquals("file contents", "Hello, world!", Files.readFirstLine(somefileTxt, Charset.defaultCharset()));
    }
}
