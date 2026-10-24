.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Correct building altitude
=========================

Overview
--------

➡️ Correct building geometries when their Z coordinate is the ground altitude by adding the building height column to each geometry vertex.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName`` — *Name of the buildings table*
   Name of the buildings table on which the geometry altitude will be corrected.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``heightColumn`` — *Height column*
   Column containing building heights in meters. Default: HEIGHT

   Type: ``String``

   Default: ``HEIGHT``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

