.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Clean Buildings Table
=====================

Overview
--------

➡️ Clean the BUILDINGS table, avoiding overlapping areas and unclosed polygons.
NoiseModelling propagation code does not support well intersecting polygons  ✅  The input table will be erased and replaced by the cleaned one.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the Buildings table.  The table must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.  The table shall contain: -  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON).-  HEIGHT : the height of the building (FLOAT)

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
