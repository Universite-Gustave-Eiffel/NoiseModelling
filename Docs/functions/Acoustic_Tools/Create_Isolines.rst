.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Create Isolines
===============

Create Isolines (Isophones)

Overview
--------

Generate isolines (isophones) by linear interpolation on triangle edges (marching-triangles). One multilines map per PERIOD and per LEVEL is created.   Main output table : ISOLINES_NOISE_MAP  with : -  PERIOD  : receivers period label (VARCHAR). -  LEVEL  : isoline value (DOUBLE). -  THE_GEOM  : MULTILINESTRING/LINESTRING geometry.   Additional output tables  : one table per PERIOD, named L<PERIOD>_ISOLINES_NOISE_MAP, containing only the isolines of that PERIOD (same structure as above).

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``trianglesTable``
   Name of the triangles table.Shall contain : PK, THE_GEOM, PK_1, PK_2, PK_3, CELL_ID.

``receiversTable``
   Name of the receivers level table.Shall contain : IDRECEIVER, PERIOD, THE_GEOM, LAEQ (or any field to contour).

Optional inputs
~~~~~~~~~~~~~~~

``fieldName``
   Receivers numeric field to contour (e.g. LAEQ). Default: LAEQ.

``isoClasses``
   Comma-separated levels. Default: 35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
