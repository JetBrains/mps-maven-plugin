# MPS Driver

MPS Driver is a Java application (i.e. it has a `main` method) that runs in the context of a particular version of MPS
(i.e. having MPS classes on class path).

`mps-maven-plugin` does not depend on this module directly or indirectly. Instead, the plugin starts `Driver` in
a separate process with appropriate class path and pass it a serialized instance of a class defined in `mps-driver-api`.

See Javadoc for more details.