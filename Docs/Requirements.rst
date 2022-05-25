Requirements
^^^^^^^^^^^^^^^^^




Java environment
~~~~~~~~~~~~~~~~~~~~

Since NoiseModelling is developped with the `Java langage`_, you will need to install the Java Virtual Machine (JVM) on your computer to use the application.


.. Warning::
    **Windows users**: If you are launching NoiseModelling thanks to the ``NoiseModelling_xxx_install.exe`` file, the JVM is already inside, so you don't have anything to do.

    **Linux or Mac users**: If not already done, you have to install the Java version v11.x. Currently only version 11 of Java is compatible (Download Java here : https://www.java.com/fr/download/)

.. _Java langage : https://en.wikipedia.org/wiki/Java_(programming_language)







Windows
-------------------

Since version 3.3.2, an executable file has been made for you ! You can go directly to step 1.

Other platforms
---------------------------------

Please install JAVA version v8.x. Currently only version 8 of Java is compatible

- Download Java here : https://www.java.com/fr/download/

- You can check if JAVA_HOME environnement variable is well settled to your last installed java folder using ``echo %JAVA_HOME%`` (windows) or ``echo $JAVA_HOME`` (linux) in your command prompt. You should have a result similar to ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\``

-  If you don't have this result, it is probably because your JAVA_HOME environnement variable is not well settled. To set you JAVA_HOME environnement variable you can adapt (with ``x`` the JAVA version number) you installed and use the following command line : ``setx JAVA_HOME  "C:\\Program Files (x86)\\Java\\jre.1.8.x_x"`` in your command prompt. You can also refer to `this document`_ for example. 

- You may have to reboot your command prompt after using the precedent command line before printing again ``echo %JAVA_HOME%`` (windows) or ``echo $JAVA_HOME`` (linux).

.. warning::
    The command promprt should print ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\`` whithout the bin directory. If JAVA_HOME is settled as ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\bin``, it will not work. It should also point to a JRE  (Java Runtime Environment) Java environnement and not JDK. 
    
.. _this document : https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html   






Docker Setup
~~~~~~~~~~~~~~~~~~~~

When a developer uses Docker (https://www.docker.com/), he creates an application or service, which he then bundles together with the associated dependencies in a container image. An image is a static representation of the application or service, its configuration and dependencies.

A docker image for the NoiseModelling v3.4.4 library has already been built. Please visit: https://github.com/tomasanda/docker-noisemodelling

.. warning::
    This docker version is made with NoiseModelling v3.4.4 which is an old release. A news Docker version with the last NoiseModelling version may be made. 