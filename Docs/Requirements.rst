Requirements
^^^^^^^^^^^^^^^^^

Java environment
~~~~~~~~~~~~~~~~~~~~

Since NoiseModelling is developped with the `Java langage`_, you will need to install the Java Runtime Environment (JRE) on your computer to use the application.

.. warning::
    **Only version 11.x of Java is compatible with NoiseModelling 4.x**. Unfortunatelay, former or newer versions are not compatible with NoiseModelling 4.x.

.. _Java langage : https://en.wikipedia.org/wiki/Java_(programming_language)




Windows
----------

If you are launching NoiseModelling thanks to the ``NoiseModelling_xxx_install.exe`` file, the JRE is already inside, so **you don't have anything to do**.

If you are not using the ``.exe`` file, you have to launch NoiseModelling thanks to the ```...\bin\startup_windows.bat`` file (in the ``NoiseModelling_xxx.zip`` release file). In this case, Java v11.x has to be installed before.


#. Download and install Java: choose between `OpenJDK`_ or `Oracle`_ versions.

#. You can check if ``JAVA_HOME`` environnement variable is well settled to your last installed Java folder using ``echo %JAVA_HOME%``  in your command prompt. You should have a result similar to ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\``.

#. If you don't have this result, it is probably because your ``JAVA_HOME`` environnement variable is not well settled. To set you ``JAVA_HOME`` environnement variable you can adapt (with ``x`` the JAVA version number) you installed and use the following command line : ``setx JAVA_HOME  "C:\\Program Files (x86)\\Java\\jre.1.8.x_x"`` in your command prompt. You can also refer to `this document`_ for example. 

#. You may have to reboot your command prompt after using the precedent command line before printing again ``echo %JAVA_HOME%``.

.. warning::
    The command promprt should print ``C:\\Program Files (x86)\\Java\\jre1.11.x_x\\`` whithout the bin directory. If ``JAVA_HOME`` is settled as ``C:\\Program Files (x86)\\Java\\jre1.11.x_x\\bin``, it will not work. It should also point to a JRE (Java Runtime Environment) Java environnement and not JDK.
    
.. _this document : https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html


Linux or Mac
-------------

If not already done, you have to install the Java version v11.x.

#. Check whether Java 11 is already installed::

      java -version

   The command should print a version starting with ``11``. Otherwise, install
   Java 11 first.

#. Download and install Java: choose between `OpenJDK`_ or `Oracle`_ versions.

#. Find the installation path to use for ``JAVA_HOME``.

   *On Linux*::

      readlink -f "$(command -v java)"

   This prints a path ending with ``/bin/java`` (for example
   ``/usr/lib/jvm/java-11-openjdk-amd64/bin/java``); ``JAVA_HOME`` is the parent
   directory of ``bin`` (here ``/usr/lib/jvm/java-11-openjdk-amd64``).

   *On macOS*::

      /usr/libexec/java_home -v 11

   This prints the directory that must be used as ``JAVA_HOME`` for Java 11.

#. Set the ``JAVA_HOME`` environment variable and update your ``PATH`` (adapt the
   path with the one found above)::

      export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
      export PATH="$JAVA_HOME/bin:$PATH"

   On macOS you can also use::

      export JAVA_HOME=$(/usr/libexec/java_home -v 11)
      export PATH="$JAVA_HOME/bin:$PATH"

#. Verify that ``JAVA_HOME`` is correctly set::

      echo $JAVA_HOME

   You should get the Java 11 directory (for example
   ``/usr/lib/jvm/java-11-openjdk-amd64/``). If this is not the case, you are
   invited to follow the steps `proposed here`_.

.. _proposed here: https://stackoverflow.com/questions/24641536/how-to-set-java-home-in-linux-for-all-users
.. _OpenJDK : https://jdk.java.net/archive/
.. _Oracle : https://www.oracle.com/fr/java/technologies/javase/jdk11-archive-downloads.html
