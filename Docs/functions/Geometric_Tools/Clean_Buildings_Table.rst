.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Clean Buildings Table
=====================

Overview
--------

➡️ Clean the BUILDINGS table, avoiding overlapping areas and unclosed polygons.
NoiseModelling propagation code does not support intersecting polygons well  ✅  The input table will be erased and replaced by the cleaned one.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName`` — *Buildings table name*
   Name of the Buildings table.  The table must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.  The table shall contain: -  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON).-  HEIGHT : the height of the building (FLOAT)

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

