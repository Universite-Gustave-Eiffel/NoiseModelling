Requirements
^^^^^^^^^^^^^^^^^

Java environment
~~~~~~~~~~~~~~~~~~~~

Since NoiseModelling is developed with the `Java langage`_, you will need to install the Java Runtime Environment (JRE) on your computer to use the application.

NoiseModelling requires Java >= 25. Any version of Java 25 or later is supported.

.. _Java langage : https://en.wikipedia.org/wiki/Java_(programming_language)

Windows
----------

If you are launching NoiseModelling thanks to the ``NoiseModelling_xxx_install.exe`` file, the JRE is already inside, so **you don't have anything to do**.

If you are not using the ``.exe`` file, you have to launch NoiseModelling thanks to the ```start_windows.bat`` file (in the ``NoiseModelling_xxx.zip`` release file). In this case, Java >= 11  has to be installed before.

Download and install Java: choose between `OpenJDK`_ or `Oracle`_ versions.

.. _this document : https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html


Linux or Mac
-------------

If not already done, you have to install the Java version >= 11.

#. Check whether Java is already installed::

      java -version

   The command should print a version starting with ``11``. Otherwise, install Java first.

#. Download and install Java: choose between `OpenJDK`_ or `Oracle`_ versions.

#. Find the installation path to use for ``JAVA_HOME``.

   *On Linux*::

      readlink -f "$(command -v java)"

   This prints a path ending with ``/bin/java`` (for example
   ``/usr/lib/jvm/java-22-openjdk-amd64/bin/java``); ``JAVA_HOME`` is the parent
   directory of ``bin`` (here ``/usr/lib/jvm/java-22-openjdk-amd64``).

   *On macOS*::

      /usr/libexec/java_home -v latest

   This prints the directory that must be used as ``JAVA_HOME`` for Java.

#. Set the ``JAVA_HOME`` environment variable and update your ``PATH`` (adapt the
   path with the one found above)::

      export JAVA_HOME=/usr/lib/jvm/java-22-openjdk-amd64
      export PATH="$JAVA_HOME/bin:$PATH"

   On macOS you can also use::

      export JAVA_HOME=$(/usr/libexec/java_home -v latest)
      export PATH="$JAVA_HOME/bin:$PATH"

#. Verify that ``JAVA_HOME`` is correctly set::

      echo $JAVA_HOME

   You should get the Java directory (for example
   ``/usr/lib/jvm/java-22-openjdk-amd64``). If this is not the case, you are
   invited to follow the steps `proposed here`_.

.. _proposed here: https://stackoverflow.com/questions/24641536/how-to-set-java-home-in-linux-for-all-users
.. _OpenJDK : https://www.azul.com/downloads/
.. _Oracle : https://www.oracle.com/java/technologies/downloads/
