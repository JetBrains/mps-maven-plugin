package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.project.structure.modules.ModuleReference;
import org.apache.log4j.BasicConfigurator;
import org.jetbrains.mps.openapi.module.SModuleReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class DependencyReadingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void configureLog4j() {
        BasicConfigurator.configure();
    }

    @AfterClass
    public static void resetLog4jConfiguration() {
        BasicConfigurator.resetConfiguration();
    }

    @Test
    public void canReadAllKindsOfDependencies() throws IOException {
        String moduleXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<module namespace=\"jetbrains.mps.baseLanguage\" type=\"language\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\">\n" +
                "  <dependencies>\n" +
                "    <module ref=\"00000000-0000-0000-0000-000000000001(module-dependency)\" />\n" +
                "  </dependencies>\n" +
                "  <sources descriptor=\"jetbrains.mps.baseLanguage.mpl\" jar=\"jetbrains.mps.baseLanguage-src.jar\" />\n" +
                "</module>";

        String mplContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<language namespace=\"jetbrains.mps.baseLanguage\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\" generatorOutputPath=\"${module}/source_gen\" languageVersion=\"4\" moduleVersion=\"0\">\n" +
                "  <generators>\n" +
                "    <generator name=\"java\" generatorUID=\"jetbrains.mps.baseLanguage#1129914002933\" uuid=\"985c8c6a-64b4-486d-a91e-7d4112742556\">\n" +
                "      <dependencies>\n" +
                "        <dependency reexport=\"false\">00000000-0000-0000-0000-000000000002(generator-dependency)</dependency>\n" +
                "      </dependencies>\n" +
                "      <languageVersions>\n" +
                "        <language slang=\"l:00000000-0000-0000-0000-000000000005:used-language-in-generator\" version=\"0\" />\n" +
                "      </languageVersions>\n" +
                "    </generator>\n" +
                "  </generators>\n" +
                "  <dependencies>\n" +
                "    <dependency reexport=\"false\">00000000-0000-0000-0000-000000000003(language-dependency)</dependency>\n" +
                "  </dependencies>\n" +
                "  <extendedLanguages>\n" +
                "    <extendedLanguage>00000000-0000-0000-0000-000000000004(extended-language)</extendedLanguage>\n" +
                "  </extendedLanguages>\n" +
                "  <languageVersions>\n" +
                "    <language slang=\"l:10000000-0000-0000-0000-000000000000:used-language-in-language\" version=\"0\" />\n" +
                "  </languageVersions>\n" +
                "</language>";

        File extracted = temporaryFolder.newFolder("extracted");
        File classesJar = temporaryFolder.newFile("extracted/jetbrains.mps.baseLanguage.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(classesJar))) {
            stream.putNextEntry(new ZipEntry("META-INF/module.xml"));
            stream.write(moduleXmlContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        File sourcesJar = temporaryFolder.newFile("extracted/jetbrains.mps.baseLanguage-src.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(sourcesJar))) {
            stream.putNextEntry(new ZipEntry("module/jetbrains.mps.baseLanguage.mpl"));
            stream.write(mplContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        Set<SModuleReference> dependencyIds = getDependencyIds(extracted);

        assertThat(dependencyIds, containsInAnyOrder(
                ModuleReference.parseReference("00000000-0000-0000-0000-000000000001(module-dependency)"),
                ModuleReference.parseReference("00000000-0000-0000-0000-000000000002(generator-dependency)"),
                ModuleReference.parseReference("00000000-0000-0000-0000-000000000003(language-dependency)"),
                ModuleReference.parseReference("00000000-0000-0000-0000-000000000004(extended-language)"),
                ModuleReference.parseReference("00000000-0000-0000-0000-000000000005(used-language-in-generator)")));
    }

    private static Set<SModuleReference> getDependencyIds(File extracted) {
        return MpsModules.getDependencyIds(getModuleHandles(extracted)).keySet();
    }

    private static Collection<ModulesMiner.ModuleHandle> getModuleHandles(File root) {
        Collection<ModulesMiner.ModuleHandle> moduleHandles;
        try (MultiMiner miner = new MultiMiner()) {
            moduleHandles = miner.collectModules(root);
        }
        return moduleHandles;
    }

    @Test
    public void willIgnoreDesignScopeDependencies() throws IOException {
        String moduleXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<module namespace=\"jetbrains.mps.baseLanguage\" type=\"language\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\">\n" +
                "  <sources descriptor=\"jetbrains.mps.baseLanguage.mpl\" jar=\"jetbrains.mps.baseLanguage-src.jar\" />\n" +
                "</module>";

        String mplContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<language namespace=\"jetbrains.mps.baseLanguage\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\" generatorOutputPath=\"${module}/source_gen\" languageVersion=\"4\" moduleVersion=\"0\">\n" +
                "  <dependencies>\n" +
                "    <dependency reexport=\"false\">00000000-0000-0000-0000-000000000001(default-dependency)</dependency>\n" +
                "    <dependency reexport=\"false\" scope=\"design\">00000000-0000-0000-0000-000000000002(design-dependency)</dependency>\n" +
                "  </dependencies>\n" +
                "</language>";

        File extracted = temporaryFolder.newFolder("extracted");
        File classesJar = temporaryFolder.newFile("extracted/jetbrains.mps.baseLanguage.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(classesJar))) {
            stream.putNextEntry(new ZipEntry("META-INF/module.xml"));
            stream.write(moduleXmlContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        File sourcesJar = temporaryFolder.newFile("extracted/jetbrains.mps.baseLanguage-src.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(sourcesJar))) {
            stream.putNextEntry(new ZipEntry("module/jetbrains.mps.baseLanguage.mpl"));
            stream.write(mplContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        Set<SModuleReference> dependencyIds = getDependencyIds(extracted);

        assertThat(dependencyIds, not(hasItem(
                hasProperty("moduleName", containsString("design-dependency")))));
    }
}
