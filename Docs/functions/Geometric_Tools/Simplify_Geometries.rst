.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

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

``tableName`` — *Name of the table*
   Name of the table on which geometries will be simplified

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``distanceTolerance`` — *Distance tolerance*
   Sets the tolerance distance for the simplification (FLOAT)

   Type: ``Double``

   Default: ``1``

``preserveTopology`` — *Preserve topology ?*
   Do you want to preserve topology?

   Type: ``Boolean``

   Default: ``false``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

