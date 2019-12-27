[![JetBrains team project](http://jb.gg/badges/team-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
# MPS Maven Plugin
`mps-maven-plugin` generates code from [JetBrains MPS](http://www.jetbrains.com/mps/) models created using MPS language
plugins for IntelliJ IDEA. The plugin does not require MPS to be installed on the machine and thus helps integrate MPS
into Maven-based continuous integration builds.

**PRE-RELEASE NOTE:** The project is currently pre-release software with unstable interfaces until we figure out what
the best way is to package artifacts, how dependencies should be specified, what the most often encountered usage
scenarios are, and so on. Everything may change until then.

See the [documentation site](http://jetbrains.github.io/mps-maven-plugin) for usage instructions and reference.

#### Compatibility with versions of MPS
Also with regard to LTS versions of jvm, 8 and 11

 * for MPS pre 2018.2 use 0.1-SNAPSHOT
 * for MPS 2018.2/3 use 0.2-SNAPSHOT
 * for MPS 2019.1/2 use 0.3-SNAPSHOT
   * 19.1 works on jvm 8 but may have problems on jvm 11
   * 19.2 works only on jvm 11
 * for MPS 2019.3 and later use 0.4-SNAPSHOT
   * only jvm 11

#### Building

Optionally, for one of the older versions 0.X-SNAPSHOT do

`git checkout 0.X`

You have to have installed a suitable version of MPS. For that, see
[mps maven deployer](https://github.com/JetBrains/mps-maven-deployer)

Building and installing this maven plugin

`mvn clean install`
