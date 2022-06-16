Requirements
^^^^^^^^^^^^^^^^^

Java environment
~~~~~~~~~~~~~~~~~~~~

Since NoiseModelling is developped with the `Java langage`_, you will need to install the Java Runtime Environment (JRE) on your computer to use the application.

.. warning::
    Only version 11.x of Java is compatible with NoiseModelling 4.x

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


#. Download and install Java: choose between `OpenJDK`_ or `Oracle`_ versions.

#. You can check if ``JAVA_HOME`` environnement variable is well settled to your installed v11.x Java folder using ``echo $JAVA_HOME`` in your command prompt. You should have a result similar to ``/usr/lib/jvm/java-11-openjdk-amd64/``.

#. If you don't have this result, it is probably because your ``JAVA_HOME`` environnement variable is not well settled. In this case, you are invited to follow the steps `proposed here`_.

#. Once done, you may have to reboot your command prompt (or maybe disconnect/reconnect your session) after using the precedent command line before printing again ``echo $JAVA_HOME``.

.. _proposed here: https://stackoverflow.com/questions/24641536/how-to-set-java-home-in-linux-for-all-users


.. _OpenJDK : https://openjdk.java.net/
.. _Oracle : https://www.java.com/fr/download/