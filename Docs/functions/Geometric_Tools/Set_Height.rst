.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Set Height
==========

Set_Height

Overview
--------

➡️ Update the geometry by adding a height from the column in the input table that contains the heights or elevations or from a static value.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the table on which the height will be modified.

Optional inputs
~~~~~~~~~~~~~~~

``height``
   New height for the input table (in meters) (FLOAT)

``heightColumn``
   The column name in the input table that contains the heights

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
