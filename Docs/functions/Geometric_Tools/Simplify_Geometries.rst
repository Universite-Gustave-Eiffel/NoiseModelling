.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Simplify Geometries
===================

Overview
--------

➡️ Use Douglas-Peucker algorithm to simplify geometries in the selected table.
✅ The input table geometries will be updated.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the table on which geometries will be simplified.

Optional inputs
~~~~~~~~~~~~~~~

``distanceTolerance``
   Sets the tolerance distance for the simplification (FLOAT).

   Default: ``1``

``preserveTopology``
   Do you want to preserve topology?

   Default: ``false``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
