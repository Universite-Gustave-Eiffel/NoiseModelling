.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Add Laeq Leq columns
====================

Add Leq and LAeq columns

Overview
--------

➡️ Add the columns Leq and LAeq to a table with octave band values from 63 Hz to 8000 Hz.
The columns of the table should be named HZ63, HZ125,..., HZ8000 with an HZ prefix that can be changed.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``prefix``
   Prefix of the columns containing the octave bands. (STRING) For example: HZ

``tableName``
   Name of the table on which Leq and LAeq columns will be added.

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
