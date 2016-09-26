# Module Archive Format

## Structure

The structure of a module archive is very simple. It is a jar (zip) file containing other jars. The file has a `.mar`
extension to distinguish it from plain jar files that contain classes or resources directly. For example:

```
  my-modules.mar
    mylanguage.jar
    mylanguage-src.jar
    mylanguage-generator.jar
    mysolution.jar
    mysolution-src.jar
```

Hierarchy of folders inside the module archive is supported. Example:

```
  my-modules.mar
    languages/
      mylanguage.jar
      mylanguage-src.jar
      mylanguage-generator.jar
    solutions/
      mysolution.jar
      mysolution-src.jar
```

## Rationale

Maven expects its artifacts to be self-contained: they should not reference any other artifact or file by name, only via
Maven coordinates. MPS modules violate this restriction. A language module is represented by three jar files: the first
contains the compiled classes, another jar contains the sources and the module descriptor, and a third jar file contains
the generator model and classes. The structure of a solution module is the same apart from not containing a generator.

The self-containment is further violated by module jars referencing each other: the classes jar references the sources
jar by name, and the generator jar is looked up by naming convention.

Since Maven stores all files in its repository under certain fixed names, it is not possible to store the MPS module jar
files in the Maven repository directly. The solution that this plugin resorts to is to bundle individual module files in
one _module archive_, or mar. As a side effect, it is possible to bundle several modules into one mar.
