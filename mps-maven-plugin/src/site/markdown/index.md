# MPS Maven Plugin
The MPS Maven Plugin is used to generate code from [JetBrains MPS](http://www.jetbrains.com/mps/) models created using
MPS language plugins for IntelliJ IDEA. The plugin does not require MPS to be installed on the machine and thus helps
integrate MPS into Maven-based continuous integration builds.

# Goals Overview
The MPS Maven Plugin has a single goal, [`mps:generate-java`](generate-java-mojo.html).

# Prerequisites
The plugin requires an MPS distribution deployed as an artifact to a Maven repository.
[MPS Maven Deployer](https://github.com/JetBrains/mps-maven-deployer) is a Gradle script that automates downloading an
MPS distribution, repackaging it as a Maven artifact and deploying it to a Maven repository.

# Generating Java From MPS Models
1. Deploy the MPS modules (languages and their required solutions) to the Maven repository using
   [MPS Maven Deployer](https://github.com/JetBrains/mps-maven-deployer). Deploy any other required modules either as
   individual JARs or as ZIPs of JARs. See [Packaging Modules For `mps-maven-plugin`](packaging-modules.html)
2. Put your models (`*.mps`) into the `src/main/mps` folder.
3. Add the plugin to your `pom.xml` and configure dependencies on the artifacts deployed in step 1. For example:

    ```xml
    <plugin>
       <groupId>org.jetbrains.mps</groupId>
       <artifactId>mps-maven-plugin</artifactId>
       <version>${mps-maven-plugin.version}</version>
       <executions>
           <execution>
               <goals>
                   <goal>generate-java</goal>
               </goals>
                <configuration>
                    <mps>
                        <groupId>org.jetbrains.mps</groupId>
                        <artifactId>mps</artifactId>
                        <version>3.4.1</version>
                        <type>zip</type>
                    </mps>
                    <dependencies>
                        <dependency>
                            <groupId>com.acme</groupId>
                            <artifactId>awesome-languages</artifactId>
                            <version>1.0</version>
                            <type>zip</type>
                        </dependency>
                    </dependencies>
                </configuration>
           </execution>
       </executions>
    </plugin>
    ```

4. Run `mvn generate-sources` (or any other Maven goal that includes `generate-sources`).
