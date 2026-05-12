.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Add Laeq Leq columns
====================

Add LAeq and Leq columns

Overview
--------

➡️ Add the columns LAeq and Leq to a table with octave band values from 63 Hz to 8000 Hz.
The columns of the table should be named HZ63, HZ125,..., HZ8000 with an HZ prefix that can be changed.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``prefix`` — *Prefix of the frequency bands column*
   Prefix of the columns containing the octave bands. (STRING) For example: HZ

   Type: ``String``

``tableName`` — *Name of the table*
   Name of the table on which LAeq and Leq columns will be added.

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

