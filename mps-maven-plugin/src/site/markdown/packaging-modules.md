# Packaging Modules For `mps-maven-plugin`

## Structure

The structure of a module archive is very simple. It is a ZIP file containing JAR files. The plugin unpacks each dependency
of type `zip` to a separate temporary folder and passes any contained JAR files to MPS as libraries to use during
generation. Any `jar` dependencies are passed to MPS untouched.

Example archive structure:

```
  my-modules.zip
    mylanguage.jar
    mylanguage-src.jar
    mylanguage-generator.jar
    mysolution.jar
    mysolution-src.jar
```

Hierarchy of folders inside the module archive is supported. Example:

```
  my-modules.zip
    languages/
      mylanguage.jar
      mylanguage-src.jar
      mylanguage-generator.jar
    solutions/
      mysolution.jar
      mysolution-src.jar
```

## Why Can't I Simply Publish Individual JARs?

Maven expects its artifacts to be self-contained: they should not reference any other artifact or file by name, only via
Maven coordinates. However, MPS modules (especially languages) violate this: a module (language or solution) references
its sources by name, and a language references its generator by naming convention.

Since Maven stores all files in its repository under certain fixed names, it is not possible to store the MPS module jar
files in the Maven repository directly. The solution that this plugin resorts to is to bundle individual module files in
one ZIP. As a side effect, it is possible to bundle several languages/solutions into one ZIP.
