Maven Plugin
============

In addition to `SBT <https://twitter.github.io/scrooge/SBTPlugin.html>`_,
Scrooge is also capable of integrating with `Apache Maven
<https://maven.apache.org>`_.

Code Dependency Additions
~~~~~~~~~~~~~~~~~~~~~~~~~

To add Scrooge to an existing Maven project, add the following dependencies
to the project's 'pom.xml' file:

* Apache Thrift
* Scrooge Core
* Finagle Thrift (Required if using Finagle)

.. includecode:: ../../../demos/scrooge-maven-demo/pom.xml#dependencies
   :comment: <!--
   :endcomment: -->

Plugin Addition
~~~~~~~~~~~~~~~

Then add the 'scrooge-maven-plugin' itself to the 'build/plugins' section
of the project's 'pom.xml' file.

.. includecode:: ../../../demos/scrooge-maven-demo/pom.xml#plugin
   :comment: <!--
   :endcomment: -->

Scrooge Maven Plugin Configuration Options
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the 'build/plugin' section, the behavior of the 'scrooge-maven-plugin'
can be modified by adding additional `Scrooge command line options
<https://twitter.github.io/scrooge/CommandLine.html#scrooge-generator>`_ to
the 'thriftOpts' section, as indicated above by '--finagle'. For reference,
a working example is provided in `scrooge-maven-demo
<https://github.com/twitter/scrooge/tree/master/demos/scrooge-maven-demo>`_.
