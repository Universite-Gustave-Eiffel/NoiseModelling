.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Import Symuvia
==============

Import Symuvia File

Overview
--------

➡️ Import Symuvia outputs (as .xml) into the database

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile``
   📂 Path of the input File (including extension .xml) For example: c:/home/mysymuviafile.xml

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   Symuvia output file SRID

   Default: ``French``

``tableName``
   Do not write the name of a table that contains a space

   Default: ``it will take the name of the file without its extension``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
