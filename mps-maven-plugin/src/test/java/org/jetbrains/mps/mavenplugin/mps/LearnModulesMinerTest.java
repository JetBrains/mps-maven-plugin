package org.jetbrains.mps.mavenplugin.mps;

import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.project.structure.modules.DeploymentDescriptor;
import jetbrains.mps.project.structure.modules.LanguageDescriptor;
import jetbrains.mps.project.structure.modules.ModuleDescriptor;
import jetbrains.mps.vfs.impl.IoFile;
import org.apache.log4j.BasicConfigurator;
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
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class LearnModulesMinerTest {
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
    public void invalidModule() throws IOException {
        File classesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(classesJar))) {
            stream.putNextEntry(new ZipEntry("META-INF/module.xml"));
        }

        File sourcesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage-src.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(sourcesJar))) {
            stream.putNextEntry(new ZipEntry("module/jetbrains.mps.baseLanguage.mpl"));
        }

        assertThat(collectModules(), is(empty()));
    }

    @Test
    public void moduleWithSources() throws IOException {
        String moduleXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<module namespace=\"jetbrains.mps.baseLanguage\" type=\"language\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\">\n" +
                "  <dependencies>\n" +
                "    <module ref=\"6354ebe7-c22a-4a0f-ac54-50b52ab9b065(JDK)\" />\n" +
                "    <module ref=\"6ed54515-acc8-4d1e-a16c-9fd6cfe951ea(MPS.Core)\" />\n" +
                "    <module ref=\"1ed103c3-3aa6-49b7-9c21-6765ee11f224(MPS.Editor)\" />\n" +
                "    <module ref=\"8865b7a8-5271-43d3-884c-6fd1d9cfdd34(MPS.OpenAPI)\" />\n" +
                "    <module ref=\"742f6602-5a2f-4313-aa6e-ae1cd4ffdc61(MPS.Platform)\" />\n" +
                "    <module ref=\"4c6a28d1-2c60-478d-b36e-db9b3cbb21fb(closures.runtime)\" />\n" +
                "    <module ref=\"9b80526e-f0bf-4992-bdf5-cee39c1833f3(collections.runtime)\" />\n" +
                "    <module ref=\"a3e4657f-a76c-45bb-bbda-c764596ecc65(jetbrains.mps.baseLanguage.logging.runtime)\" />\n" +
                "    <module ref=\"23949432-aaff-4c03-b7da-26e4e956ccea(jetbrains.mps.baseLanguage.regexp.runtime)\" />\n" +
                "    <module ref=\"d44dab97-aaac-44cb-9745-8a14db674c03(jetbrains.mps.baseLanguage.tuples.runtime)\" />\n" +
                "    <module ref=\"2af156ab-65c1-4a62-bd0d-ea734f71eab6(jetbrains.mps.dataFlow.runtime)\" />\n" +
                "    <module ref=\"34e84b8f-afa8-4364-abcd-a279fddddbe7(jetbrains.mps.editor.runtime)\" />\n" +
                "    <module ref=\"2d3c70e9-aab2-4870-8d8d-6036800e4103(jetbrains.mps.kernel)\" />\n" +
                "    <module ref=\"d936855b-48da-4812-a8a0-2bfddd633ac5(jetbrains.mps.lang.behavior.api)\" />\n" +
                "    <module ref=\"d936855b-48da-4812-a8a0-2bfddd633ac4(jetbrains.mps.lang.behavior.runtime)\" />\n" +
                "    <module ref=\"528ff3b9-5fc4-40dd-931f-c6ce3650640e(jetbrains.mps.lang.migration.runtime)\" />\n" +
                "    <module ref=\"d7eb0a2a-bd50-4576-beae-e4a89db35f20(jetbrains.mps.lang.scopes.runtime)\" />\n" +
                "    <module ref=\"31f56055-9d30-42b3-a2b1-fb3f554d7075(jetbrains.mps.lang.smodel.query.runtime)\" />\n" +
                "    <module ref=\"8fe4c62a-2020-4ff4-8eda-f322a55bdc9f(jetbrains.mps.refactoring.runtime)\" />\n" +
                "    <module ref=\"9a4afe51-f114-4595-b5df-048ce3c596be(jetbrains.mps.runtime)\" />\n" +
                "  </dependencies>\n" +
                "  <sources descriptor=\"jetbrains.mps.baseLanguage.mpl\" jar=\"jetbrains.mps.baseLanguage-src.jar\" />\n" +
                "</module>";

        String mplContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<language namespace=\"jetbrains.mps.baseLanguage\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\" generatorOutputPath=\"${module}/source_gen\" languageVersion=\"4\" moduleVersion=\"0\">\n" +
                "  <models>\n" +
                "    <modelRoot contentPath=\"${module}\" type=\"default\">\n" +
                "      <sourceRoot location=\"languageModels\" />\n" +
                "    </modelRoot>\n" +
                "  </models>\n" +
                "  <accessoryModels>\n" +
                "    <model modelUID=\"6354ebe7-c22a-4a0f-ac54-50b52ab9b065/java:java.lang(JDK/)\" />\n" +
                "  </accessoryModels>\n" +
                "  <generators>\n" +
                "    <generator name=\"java\" generatorUID=\"jetbrains.mps.baseLanguage#1129914002933\" uuid=\"985c8c6a-64b4-486d-a91e-7d4112742556\">\n" +
                "      <models>\n" +
                "        <modelRoot contentPath=\"${module}\" type=\"default\">\n" +
                "          <sourceRoot location=\"generator/java/templates\" />\n" +
                "        </modelRoot>\n" +
                "      </models>\n" +
                "      <external-templates />\n" +
                "      <dependencies>\n" +
                "        <dependency reexport=\"false\">6ed54515-acc8-4d1e-a16c-9fd6cfe951ea(MPS.Core)</dependency>\n" +
                "        <dependency reexport=\"false\">df345b11-b8c7-4213-ac66-48d2a9b75d88(jetbrains.mps.baseLanguageInternal)</dependency>\n" +
                "        <dependency reexport=\"false\">2d3c70e9-aab2-4870-8d8d-6036800e4103(jetbrains.mps.kernel)</dependency>\n" +
                "      </dependencies>\n" +
                "      <usedDevKits>\n" +
                "        <usedDevKit>fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)</usedDevKit>\n" +
                "      </usedDevKits>\n" +
                "      <languageVersions>\n" +
                "        <language slang=\"l:f3061a53-9226-4cc5-a443-f952ceaf5816:jetbrains.mps.baseLanguage\" version=\"4\" />\n" +
                "        <language slang=\"l:ed6d7656-532c-4bc2-81d1-af945aeb8280:jetbrains.mps.baseLanguage.blTypes\" version=\"0\" />\n" +
                "        <language slang=\"l:fd392034-7849-419d-9071-12563d152375:jetbrains.mps.baseLanguage.closures\" version=\"0\" />\n" +
                "        <language slang=\"l:83888646-71ce-4f1c-9c53-c54016f6ad4f:jetbrains.mps.baseLanguage.collections\" version=\"0\" />\n" +
                "        <language slang=\"l:f2801650-65d5-424e-bb1b-463a8781b786:jetbrains.mps.baseLanguage.javadoc\" version=\"2\" />\n" +
                "        <language slang=\"l:760a0a8c-eabb-4521-8bfd-65db761a9ba3:jetbrains.mps.baseLanguage.logging\" version=\"0\" />\n" +
                "        <language slang=\"l:a247e09e-2435-45ba-b8d2-07e93feba96a:jetbrains.mps.baseLanguage.tuples\" version=\"0\" />\n" +
                "        <language slang=\"l:df345b11-b8c7-4213-ac66-48d2a9b75d88:jetbrains.mps.baseLanguageInternal\" version=\"0\" />\n" +
                "        <language slang=\"l:ceab5195-25ea-4f22-9b92-103b95ca8c0c:jetbrains.mps.lang.core\" version=\"1\" />\n" +
                "        <language slang=\"l:b401a680-8325-4110-8fd3-84331ff25bef:jetbrains.mps.lang.generator\" version=\"0\" />\n" +
                "        <language slang=\"l:d7706f63-9be2-479c-a3da-ae92af1e64d5:jetbrains.mps.lang.generator.generationContext\" version=\"0\" />\n" +
                "        <language slang=\"l:3a13115c-633c-4c5c-bbcc-75c4219e9555:jetbrains.mps.lang.quotation\" version=\"0\" />\n" +
                "        <language slang=\"l:7866978e-a0f0-4cc7-81bc-4d213d9375e1:jetbrains.mps.lang.smodel\" version=\"4\" />\n" +
                "        <language slang=\"l:c72da2b9-7cce-4447-8389-f407dc1158b7:jetbrains.mps.lang.structure\" version=\"3\" />\n" +
                "        <language slang=\"l:9ded098b-ad6a-4657-bfd9-48636cfe8bc3:jetbrains.mps.lang.traceable\" version=\"0\" />\n" +
                "        <language slang=\"l:7a5dda62-9140-4668-ab76-d5ed1746f2b2:jetbrains.mps.lang.typesystem\" version=\"0\" />\n" +
                "      </languageVersions>\n" +
                "      <dependencyVersions>\n" +
                "        <module reference=\"3f233e7f-b8a6-46d2-a57f-795d56775243(Annotations)\" version=\"0\" />\n" +
                "        <module reference=\"6354ebe7-c22a-4a0f-ac54-50b52ab9b065(JDK)\" version=\"0\" />\n" +
                "        <module reference=\"6ed54515-acc8-4d1e-a16c-9fd6cfe951ea(MPS.Core)\" version=\"0\" />\n" +
                "        <module reference=\"8865b7a8-5271-43d3-884c-6fd1d9cfdd34(MPS.OpenAPI)\" version=\"0\" />\n" +
                "        <module reference=\"f3061a53-9226-4cc5-a443-f952ceaf5816(jetbrains.mps.baseLanguage)\" version=\"0\" />\n" +
                "        <module reference=\"985c8c6a-64b4-486d-a91e-7d4112742556(jetbrains.mps.baseLanguage#1129914002933)\" version=\"0\" />\n" +
                "        <module reference=\"ed6d7656-532c-4bc2-81d1-af945aeb8280(jetbrains.mps.baseLanguage.blTypes)\" version=\"0\" />\n" +
                "        <module reference=\"e39e4a59-8cb6-498e-860e-8fa8361c0d90(jetbrains.mps.baseLanguage.scopes)\" version=\"0\" />\n" +
                "        <module reference=\"df345b11-b8c7-4213-ac66-48d2a9b75d88(jetbrains.mps.baseLanguageInternal)\" version=\"0\" />\n" +
                "        <module reference=\"2d3c70e9-aab2-4870-8d8d-6036800e4103(jetbrains.mps.kernel)\" version=\"0\" />\n" +
                "        <module reference=\"ceab5195-25ea-4f22-9b92-103b95ca8c0c(jetbrains.mps.lang.core)\" version=\"0\" />\n" +
                "        <module reference=\"d7eb0a2a-bd50-4576-beae-e4a89db35f20(jetbrains.mps.lang.scopes.runtime)\" version=\"0\" />\n" +
                "        <module reference=\"c72da2b9-7cce-4447-8389-f407dc1158b7(jetbrains.mps.lang.structure)\" version=\"0\" />\n" +
                "        <module reference=\"9ded098b-ad6a-4657-bfd9-48636cfe8bc3(jetbrains.mps.lang.traceable)\" version=\"0\" />\n" +
                "      </dependencyVersions>\n" +
                "      <mapping-priorities />\n" +
                "    </generator>\n" +
                "  </generators>\n" +
                "  <sourcePath />\n" +
                "  <dependencies>\n" +
                "    <dependency reexport=\"false\">2d3c70e9-aab2-4870-8d8d-6036800e4103(jetbrains.mps.kernel)</dependency>\n" +
                "    <dependency reexport=\"false\">df9d410f-2ebb-43f7-893a-483a4f085250(jetbrains.mps.smodel.resources)</dependency>\n" +
                "    <dependency reexport=\"false\">ceab5195-25ea-4f22-9b92-103b95ca8c0c(jetbrains.mps.lang.core)</dependency>\n" +
                "    <dependency reexport=\"false\">0eddeefa-c2d6-4437-bc2c-de50fd4ce470(jetbrains.mps.lang.script)</dependency>\n" +
                "    <dependency reexport=\"false\">498d89d2-c2e9-11e2-ad49-6cf049e62fe5(MPS.IDEA)</dependency>\n" +
                "    <dependency reexport=\"false\">c72da2b9-7cce-4447-8389-f407dc1158b7(jetbrains.mps.lang.structure)</dependency>\n" +
                "    <dependency reexport=\"false\">af65afd8-f0dd-4942-87d9-63a55f2a9db1(jetbrains.mps.lang.behavior)</dependency>\n" +
                "    <dependency reexport=\"false\">b401a680-8325-4110-8fd3-84331ff25bef(jetbrains.mps.lang.generator)</dependency>\n" +
                "    <dependency reexport=\"false\">9ded098b-ad6a-4657-bfd9-48636cfe8bc3(jetbrains.mps.lang.traceable)</dependency>\n" +
                "    <dependency reexport=\"false\">d7a92d38-f7db-40d0-8431-763b0c3c9f20(jetbrains.mps.lang.intentions)</dependency>\n" +
                "    <dependency reexport=\"false\">6ed54515-acc8-4d1e-a16c-9fd6cfe951ea(MPS.Core)</dependency>\n" +
                "    <dependency reexport=\"false\">a1250a4d-c090-42c3-ad7c-d298a3357dd4(jetbrains.mps.make.runtime)</dependency>\n" +
                "    <dependency reexport=\"false\">b83431fe-5c8f-40bc-8a36-65e25f4dd253(jetbrains.mps.lang.textGen)</dependency>\n" +
                "    <dependency reexport=\"false\">d7eb0a2a-bd50-4576-beae-e4a89db35f20(jetbrains.mps.lang.scopes.runtime)</dependency>\n" +
                "    <dependency reexport=\"true\">e39e4a59-8cb6-498e-860e-8fa8361c0d90(jetbrains.mps.baseLanguage.scopes)</dependency>\n" +
                "    <dependency reexport=\"false\">443f4c36-fcf5-4eb6-9500-8d06ed259e3e(jetbrains.mps.baseLanguage.classifiers)</dependency>\n" +
                "    <dependency reexport=\"false\">3f233e7f-b8a6-46d2-a57f-795d56775243(Annotations)</dependency>\n" +
                "    <dependency reexport=\"false\">c7d01124-66d5-486d-8b50-7fdccb60b839(jetbrains.mps.baseLanguage.util)</dependency>\n" +
                "    <dependency reexport=\"false\">7a5dda62-9140-4668-ab76-d5ed1746f2b2(jetbrains.mps.lang.typesystem)</dependency>\n" +
                "    <dependency reexport=\"false\">28f9e497-3b42-4291-aeba-0a1039153ab1(jetbrains.mps.lang.plugin)</dependency>\n" +
                "    <dependency reexport=\"false\">3a13115c-633c-4c5c-bbcc-75c4219e9555(jetbrains.mps.lang.quotation)</dependency>\n" +
                "    <dependency reexport=\"false\">a247e09e-2435-45ba-b8d2-07e93feba96a(jetbrains.mps.baseLanguage.tuples)</dependency>\n" +
                "    <dependency reexport=\"false\">af19274f-5f89-42dd-8f3c-c9932448f7f2(jetbrains.mps.analyzers.runtime)</dependency>\n" +
                "    <dependency reexport=\"false\">f2801650-65d5-424e-bb1b-463a8781b786(jetbrains.mps.baseLanguage.javadoc)</dependency>\n" +
                "    <dependency reexport=\"false\">742f6602-5a2f-4313-aa6e-ae1cd4ffdc61(MPS.Platform)</dependency>\n" +
                "    <dependency reexport=\"false\">2af156ab-65c1-4a62-bd0d-ea734f71eab6(jetbrains.mps.dataFlow.runtime)</dependency>\n" +
                "    <dependency reexport=\"false\">1ed103c3-3aa6-49b7-9c21-6765ee11f224(MPS.Editor)</dependency>\n" +
                "    <dependency reexport=\"false\">20c6e580-bdc5-4067-8049-d7e3265a86de(jetbrains.mps.typesystemEngine)</dependency>\n" +
                "    <dependency reexport=\"true\">6354ebe7-c22a-4a0f-ac54-50b52ab9b065(JDK)</dependency>\n" +
                "    <dependency reexport=\"false\">83888646-71ce-4f1c-9c53-c54016f6ad4f(jetbrains.mps.baseLanguage.collections)</dependency>\n" +
                "    <dependency reexport=\"false\">982eb8df-2c96-4bd7-9963-11712ea622e5(jetbrains.mps.lang.resources)</dependency>\n" +
                "    <dependency reexport=\"false\">18bc6592-03a6-4e29-a83a-7ff23bde13ba(jetbrains.mps.lang.editor)</dependency>\n" +
                "    <dependency reexport=\"false\">8865b7a8-5271-43d3-884c-6fd1d9cfdd34(MPS.OpenAPI)</dependency>\n" +
                "    <dependency reexport=\"false\">7866978e-a0f0-4cc7-81bc-4d213d9375e1(jetbrains.mps.lang.smodel)</dependency>\n" +
                "    <dependency reexport=\"false\">aee9cad2-acd4-4608-aef2-0004f6a1cdbd(jetbrains.mps.lang.actions)</dependency>\n" +
                "    <dependency reexport=\"false\">a0c108f0-1637-416e-a249-3effbaa4c998(jetbrains.mps.baseLanguage.search)</dependency>\n" +
                "    <dependency reexport=\"false\">34e84b8f-afa8-4364-abcd-a279fddddbe7(jetbrains.mps.editor.runtime)</dependency>\n" +
                "    <dependency reexport=\"false\">f3061a53-9226-4cc5-a443-f952ceaf5816(jetbrains.mps.baseLanguage)</dependency>\n" +
                "    <dependency reexport=\"false\">528ff3b9-5fc4-40dd-931f-c6ce3650640e(jetbrains.mps.lang.migration.runtime)</dependency>\n" +
                "  </dependencies>\n" +
                "  <usedDevKits>\n" +
                "    <usedDevKit>fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)</usedDevKit>\n" +
                "    <usedDevKit>2677cb18-f558-4e33-bc38-a5139cee06dc(jetbrains.mps.devkit.language-design)</usedDevKit>\n" +
                "  </usedDevKits>\n" +
                "  <languageVersions>\n" +
                "    <language slang=\"l:f3061a53-9226-4cc5-a443-f952ceaf5816:jetbrains.mps.baseLanguage\" version=\"4\" />\n" +
                "    <language slang=\"l:ed6d7656-532c-4bc2-81d1-af945aeb8280:jetbrains.mps.baseLanguage.blTypes\" version=\"0\" />\n" +
                "    <language slang=\"l:774bf8a0-62e5-41e1-af63-f4812e60e48b:jetbrains.mps.baseLanguage.checkedDots\" version=\"0\" />\n" +
                "    <language slang=\"l:443f4c36-fcf5-4eb6-9500-8d06ed259e3e:jetbrains.mps.baseLanguage.classifiers\" version=\"0\" />\n" +
                "    <language slang=\"l:fd392034-7849-419d-9071-12563d152375:jetbrains.mps.baseLanguage.closures\" version=\"0\" />\n" +
                "    <language slang=\"l:83888646-71ce-4f1c-9c53-c54016f6ad4f:jetbrains.mps.baseLanguage.collections\" version=\"0\" />\n" +
                "    <language slang=\"l:f2801650-65d5-424e-bb1b-463a8781b786:jetbrains.mps.baseLanguage.javadoc\" version=\"2\" />\n" +
                "    <language slang=\"l:c7d5b9dd-a05f-4be2-bc73-f2e16994cc67:jetbrains.mps.baseLanguage.lightweightdsl\" version=\"1\" />\n" +
                "    <language slang=\"l:760a0a8c-eabb-4521-8bfd-65db761a9ba3:jetbrains.mps.baseLanguage.logging\" version=\"0\" />\n" +
                "    <language slang=\"l:daafa647-f1f7-4b0b-b096-69cd7c8408c0:jetbrains.mps.baseLanguage.regexp\" version=\"0\" />\n" +
                "    <language slang=\"l:a247e09e-2435-45ba-b8d2-07e93feba96a:jetbrains.mps.baseLanguage.tuples\" version=\"0\" />\n" +
                "    <language slang=\"l:aee9cad2-acd4-4608-aef2-0004f6a1cdbd:jetbrains.mps.lang.actions\" version=\"0\" />\n" +
                "    <language slang=\"l:f159adf4-3c93-40f9-9c5a-1f245a8697af:jetbrains.mps.lang.aspect\" version=\"0\" />\n" +
                "    <language slang=\"l:af65afd8-f0dd-4942-87d9-63a55f2a9db1:jetbrains.mps.lang.behavior\" version=\"0\" />\n" +
                "    <language slang=\"l:fe9d76d7-5809-45c9-ae28-a40915b4d6ff:jetbrains.mps.lang.checkedName\" version=\"0\" />\n" +
                "    <language slang=\"l:3f4bc5f5-c6c1-4a28-8b10-c83066ffa4a1:jetbrains.mps.lang.constraints\" version=\"0\" />\n" +
                "    <language slang=\"l:ceab5195-25ea-4f22-9b92-103b95ca8c0c:jetbrains.mps.lang.core\" version=\"1\" />\n" +
                "    <language slang=\"l:7fa12e9c-b949-4976-b4fa-19accbc320b4:jetbrains.mps.lang.dataFlow\" version=\"0\" />\n" +
                "    <language slang=\"l:97a52717-898f-4598-8150-573d9fd03868:jetbrains.mps.lang.dataFlow.analyzers\" version=\"0\" />\n" +
                "    <language slang=\"l:18bc6592-03a6-4e29-a83a-7ff23bde13ba:jetbrains.mps.lang.editor\" version=\"3\" />\n" +
                "    <language slang=\"l:64d34fcd-ad02-4e73-aff8-a581124c2e30:jetbrains.mps.lang.findUsages\" version=\"0\" />\n" +
                "    <language slang=\"l:d7a92d38-f7db-40d0-8431-763b0c3c9f20:jetbrains.mps.lang.intentions\" version=\"0\" />\n" +
                "    <language slang=\"l:90746344-04fd-4286-97d5-b46ae6a81709:jetbrains.mps.lang.migration\" version=\"0\" />\n" +
                "    <language slang=\"l:d4615e3b-d671-4ba9-af01-2b78369b0ba7:jetbrains.mps.lang.pattern\" version=\"1\" />\n" +
                "    <language slang=\"l:28f9e497-3b42-4291-aeba-0a1039153ab1:jetbrains.mps.lang.plugin\" version=\"0\" />\n" +
                "    <language slang=\"l:3a13115c-633c-4c5c-bbcc-75c4219e9555:jetbrains.mps.lang.quotation\" version=\"0\" />\n" +
                "    <language slang=\"l:3ecd7c84-cde3-45de-886c-135ecc69b742:jetbrains.mps.lang.refactoring\" version=\"0\" />\n" +
                "    <language slang=\"l:982eb8df-2c96-4bd7-9963-11712ea622e5:jetbrains.mps.lang.resources\" version=\"0\" />\n" +
                "    <language slang=\"l:d8f591ec-4d86-4af2-9f92-a9e93c803ffa:jetbrains.mps.lang.scopes\" version=\"0\" />\n" +
                "    <language slang=\"l:0eddeefa-c2d6-4437-bc2c-de50fd4ce470:jetbrains.mps.lang.script\" version=\"0\" />\n" +
                "    <language slang=\"l:13744753-c81f-424a-9c1b-cf8943bf4e86:jetbrains.mps.lang.sharedConcepts\" version=\"0\" />\n" +
                "    <language slang=\"l:7866978e-a0f0-4cc7-81bc-4d213d9375e1:jetbrains.mps.lang.smodel\" version=\"4\" />\n" +
                "    <language slang=\"l:1a8554c4-eb84-43ba-8c34-6f0d90c6e75a:jetbrains.mps.lang.smodel.query\" version=\"2\" />\n" +
                "    <language slang=\"l:c72da2b9-7cce-4447-8389-f407dc1158b7:jetbrains.mps.lang.structure\" version=\"3\" />\n" +
                "    <language slang=\"l:b83431fe-5c8f-40bc-8a36-65e25f4dd253:jetbrains.mps.lang.textGen\" version=\"0\" />\n" +
                "    <language slang=\"l:9ded098b-ad6a-4657-bfd9-48636cfe8bc3:jetbrains.mps.lang.traceable\" version=\"0\" />\n" +
                "    <language slang=\"l:7a5dda62-9140-4668-ab76-d5ed1746f2b2:jetbrains.mps.lang.typesystem\" version=\"0\" />\n" +
                "    <language slang=\"l:696c1165-4a59-463b-bc5d-902caab85dd0:jetbrains.mps.make.facet\" version=\"0\" />\n" +
                "    <language slang=\"l:95f8a3e6-f994-4ca0-a65e-763c9bae2d3b:jetbrains.mps.make.script\" version=\"0\" />\n" +
                "  </languageVersions>\n" +
                "  <dependencyVersions>\n" +
                "    <module reference=\"3f233e7f-b8a6-46d2-a57f-795d56775243(Annotations)\" version=\"0\" />\n" +
                "    <module reference=\"6354ebe7-c22a-4a0f-ac54-50b52ab9b065(JDK)\" version=\"0\" />\n" +
                "    <module reference=\"6ed54515-acc8-4d1e-a16c-9fd6cfe951ea(MPS.Core)\" version=\"0\" />\n" +
                "    <module reference=\"1ed103c3-3aa6-49b7-9c21-6765ee11f224(MPS.Editor)\" version=\"0\" />\n" +
                "    <module reference=\"498d89d2-c2e9-11e2-ad49-6cf049e62fe5(MPS.IDEA)\" version=\"0\" />\n" +
                "    <module reference=\"8865b7a8-5271-43d3-884c-6fd1d9cfdd34(MPS.OpenAPI)\" version=\"0\" />\n" +
                "    <module reference=\"742f6602-5a2f-4313-aa6e-ae1cd4ffdc61(MPS.Platform)\" version=\"0\" />\n" +
                "    <module reference=\"af19274f-5f89-42dd-8f3c-c9932448f7f2(jetbrains.mps.analyzers.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"f3061a53-9226-4cc5-a443-f952ceaf5816(jetbrains.mps.baseLanguage)\" version=\"0\" />\n" +
                "    <module reference=\"ed6d7656-532c-4bc2-81d1-af945aeb8280(jetbrains.mps.baseLanguage.blTypes)\" version=\"0\" />\n" +
                "    <module reference=\"443f4c36-fcf5-4eb6-9500-8d06ed259e3e(jetbrains.mps.baseLanguage.classifiers)\" version=\"0\" />\n" +
                "    <module reference=\"fd392034-7849-419d-9071-12563d152375(jetbrains.mps.baseLanguage.closures)\" version=\"0\" />\n" +
                "    <module reference=\"83888646-71ce-4f1c-9c53-c54016f6ad4f(jetbrains.mps.baseLanguage.collections)\" version=\"0\" />\n" +
                "    <module reference=\"f2801650-65d5-424e-bb1b-463a8781b786(jetbrains.mps.baseLanguage.javadoc)\" version=\"0\" />\n" +
                "    <module reference=\"c7d5b9dd-a05f-4be2-bc73-f2e16994cc67(jetbrains.mps.baseLanguage.lightweightdsl)\" version=\"0\" />\n" +
                "    <module reference=\"760a0a8c-eabb-4521-8bfd-65db761a9ba3(jetbrains.mps.baseLanguage.logging)\" version=\"0\" />\n" +
                "    <module reference=\"e39e4a59-8cb6-498e-860e-8fa8361c0d90(jetbrains.mps.baseLanguage.scopes)\" version=\"0\" />\n" +
                "    <module reference=\"a0c108f0-1637-416e-a249-3effbaa4c998(jetbrains.mps.baseLanguage.search)\" version=\"0\" />\n" +
                "    <module reference=\"a247e09e-2435-45ba-b8d2-07e93feba96a(jetbrains.mps.baseLanguage.tuples)\" version=\"0\" />\n" +
                "    <module reference=\"c7d01124-66d5-486d-8b50-7fdccb60b839(jetbrains.mps.baseLanguage.util)\" version=\"0\" />\n" +
                "    <module reference=\"2af156ab-65c1-4a62-bd0d-ea734f71eab6(jetbrains.mps.dataFlow.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"34e84b8f-afa8-4364-abcd-a279fddddbe7(jetbrains.mps.editor.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"2d3c70e9-aab2-4870-8d8d-6036800e4103(jetbrains.mps.kernel)\" version=\"0\" />\n" +
                "    <module reference=\"aee9cad2-acd4-4608-aef2-0004f6a1cdbd(jetbrains.mps.lang.actions)\" version=\"0\" />\n" +
                "    <module reference=\"af65afd8-f0dd-4942-87d9-63a55f2a9db1(jetbrains.mps.lang.behavior)\" version=\"0\" />\n" +
                "    <module reference=\"fe9d76d7-5809-45c9-ae28-a40915b4d6ff(jetbrains.mps.lang.checkedName)\" version=\"0\" />\n" +
                "    <module reference=\"ceab5195-25ea-4f22-9b92-103b95ca8c0c(jetbrains.mps.lang.core)\" version=\"0\" />\n" +
                "    <module reference=\"18bc6592-03a6-4e29-a83a-7ff23bde13ba(jetbrains.mps.lang.editor)\" version=\"0\" />\n" +
                "    <module reference=\"b401a680-8325-4110-8fd3-84331ff25bef(jetbrains.mps.lang.generator)\" version=\"0\" />\n" +
                "    <module reference=\"d7a92d38-f7db-40d0-8431-763b0c3c9f20(jetbrains.mps.lang.intentions)\" version=\"0\" />\n" +
                "    <module reference=\"528ff3b9-5fc4-40dd-931f-c6ce3650640e(jetbrains.mps.lang.migration.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"28f9e497-3b42-4291-aeba-0a1039153ab1(jetbrains.mps.lang.plugin)\" version=\"0\" />\n" +
                "    <module reference=\"3a13115c-633c-4c5c-bbcc-75c4219e9555(jetbrains.mps.lang.quotation)\" version=\"0\" />\n" +
                "    <module reference=\"982eb8df-2c96-4bd7-9963-11712ea622e5(jetbrains.mps.lang.resources)\" version=\"0\" />\n" +
                "    <module reference=\"d7eb0a2a-bd50-4576-beae-e4a89db35f20(jetbrains.mps.lang.scopes.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"0eddeefa-c2d6-4437-bc2c-de50fd4ce470(jetbrains.mps.lang.script)\" version=\"0\" />\n" +
                "    <module reference=\"13744753-c81f-424a-9c1b-cf8943bf4e86(jetbrains.mps.lang.sharedConcepts)\" version=\"0\" />\n" +
                "    <module reference=\"7866978e-a0f0-4cc7-81bc-4d213d9375e1(jetbrains.mps.lang.smodel)\" version=\"1\" />\n" +
                "    <module reference=\"c72da2b9-7cce-4447-8389-f407dc1158b7(jetbrains.mps.lang.structure)\" version=\"0\" />\n" +
                "    <module reference=\"b83431fe-5c8f-40bc-8a36-65e25f4dd253(jetbrains.mps.lang.textGen)\" version=\"0\" />\n" +
                "    <module reference=\"9ded098b-ad6a-4657-bfd9-48636cfe8bc3(jetbrains.mps.lang.traceable)\" version=\"0\" />\n" +
                "    <module reference=\"7a5dda62-9140-4668-ab76-d5ed1746f2b2(jetbrains.mps.lang.typesystem)\" version=\"0\" />\n" +
                "    <module reference=\"a1250a4d-c090-42c3-ad7c-d298a3357dd4(jetbrains.mps.make.runtime)\" version=\"0\" />\n" +
                "    <module reference=\"df9d410f-2ebb-43f7-893a-483a4f085250(jetbrains.mps.smodel.resources)\" version=\"0\" />\n" +
                "    <module reference=\"20c6e580-bdc5-4067-8049-d7e3265a86de(jetbrains.mps.typesystemEngine)\" version=\"0\" />\n" +
                "  </dependencyVersions>\n" +
                "  <runtime>\n" +
                "    <dependency reexport=\"false\">6354ebe7-c22a-4a0f-ac54-50b52ab9b065(JDK)</dependency>\n" +
                "  </runtime>\n" +
                "  <extendedLanguages>\n" +
                "    <extendedLanguage>ed6d7656-532c-4bc2-81d1-af945aeb8280(jetbrains.mps.baseLanguage.blTypes)</extendedLanguage>\n" +
                "    <extendedLanguage>ceab5195-25ea-4f22-9b92-103b95ca8c0c(jetbrains.mps.lang.core)</extendedLanguage>\n" +
                "    <extendedLanguage>9ded098b-ad6a-4657-bfd9-48636cfe8bc3(jetbrains.mps.lang.traceable)</extendedLanguage>\n" +
                "  </extendedLanguages>\n" +
                "</language>";

        File classesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(classesJar))) {
            stream.putNextEntry(new ZipEntry("META-INF/module.xml"));
            stream.write(moduleXmlContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        File sourcesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage-src.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(sourcesJar))) {
            stream.putNextEntry(new ZipEntry("module/jetbrains.mps.baseLanguage.mpl"));
            stream.write(mplContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        Collection<ModulesMiner.ModuleHandle> collectedModules = collectModules();

        assertThat(collectedModules, hasSize(1));

        ModulesMiner.ModuleHandle module = collectedModules.iterator().next();
        ModuleDescriptor descriptor = module.getDescriptor();
        assertNotNull(descriptor);
        assertThat("dependencies in module/jetbrains.mps.baseLanguage.mpl", descriptor.getDependencies(), hasSize(39));

        LanguageDescriptor languageDescriptor = (LanguageDescriptor) descriptor;
        assertThat("dependencies in module/jetbrains.mps.baseLanguage.mpl in generators",
                languageDescriptor.getGenerators().get(0).getDependencies(), hasSize(3));

        DeploymentDescriptor deploymentDescriptor = languageDescriptor.getDeploymentDescriptor();
        assertNotNull(deploymentDescriptor);
        assertThat("dependencies in META-INF/module.xml", deploymentDescriptor.getDependencies(), hasSize(20));
    }

    @Test
    public void minimalModule() throws IOException {
        String moduleXmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<module namespace=\"jetbrains.mps.baseLanguage\" type=\"language\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\">\n" +
                "  <sources descriptor=\"jetbrains.mps.baseLanguage.mpl\" jar=\"jetbrains.mps.baseLanguage-src.jar\" />\n" +
                "</module>";

        String mplContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<language namespace=\"jetbrains.mps.baseLanguage\" uuid=\"f3061a53-9226-4cc5-a443-f952ceaf5816\" generatorOutputPath=\"${module}/source_gen\" languageVersion=\"4\" moduleVersion=\"0\">\n" +
                "</language>";

        File classesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(classesJar))) {
            stream.putNextEntry(new ZipEntry("META-INF/module.xml"));
            stream.write(moduleXmlContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        File sourcesJar = temporaryFolder.newFile("jetbrains.mps.baseLanguage-src.jar");
        try (JarOutputStream stream = new JarOutputStream(new FileOutputStream(sourcesJar))) {
            stream.putNextEntry(new ZipEntry("module/jetbrains.mps.baseLanguage.mpl"));
            stream.write(mplContent.getBytes(Charset.forName("UTF-8")));
            stream.closeEntry();
        }

        Collection<ModulesMiner.ModuleHandle> collectedModules = collectModules();
        ModulesMiner.ModuleHandle module = collectedModules.iterator().next();
        LanguageDescriptor descriptor = (LanguageDescriptor) module.getDescriptor();
        assertNotNull(descriptor);
        assertThat(descriptor.getDependencies(), hasSize(0));
        assertThat(descriptor.getGenerators(), hasSize(0));
        DeploymentDescriptor deploymentDescriptor = descriptor.getDeploymentDescriptor();
        assertNotNull(deploymentDescriptor);
        assertThat(deploymentDescriptor.getDependencies(), hasSize(0));

        assertThat(collectedModules, hasSize(1));
    }

    private Collection<ModulesMiner.ModuleHandle> collectModules() {
        Collection<ModulesMiner.ModuleHandle> collectedModules;
        try (MultiMiner miner = new MultiMiner()) {
            collectedModules = miner.collectModules(temporaryFolder.getRoot());
        }
        return collectedModules;
    }
}
