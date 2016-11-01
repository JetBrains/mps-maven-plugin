This module contains functionality that runs in a separate process, in the context of a particular version of MPS. This
is in contrast to the main `mps-maven-plugin` module which is MPS version-independent. There must therefore be no
compile- or run-time dependency from `mps-maven-plugin` on this module.