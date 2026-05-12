.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Create Isolines
===============

Overview
--------

Generate isolines (isophones) by linear interpolation on triangle edges (marching-triangles). One multilines map per PERIOD and per LEVEL is created.   Main output table : ISOLINES_NOISE_MAP  with : -  PERIOD  : receivers period label (VARCHAR). -  LEVEL  : isoline value (DOUBLE). -  THE_GEOM  : MULTILINESTRING/LINESTRING geometry.   Additional output tables  : one table per PERIOD, named L<PERIOD>_ISOLINES_NOISE_MAP, containing only the isolines of that PERIOD (same structure as above).

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``receiversTable`` — *Receivers level table*
   Name of the receivers level table.Shall contain : IDRECEIVER, PERIOD, THE_GEOM, LAEQ (or any field to contour).

   Type: ``String``

``trianglesTable`` — *Triangles table*
   Name of the triangles table.Shall contain : PK, THE_GEOM, PK_1, PK_2, PK_3, CELL_ID.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``fieldName`` — *Field to contour*
   Receivers numeric field to contour (e.g. LAEQ).

   Type: ``String``

   Default: ``LAEQ``

``isoClasses`` — *Iso levels (dB)*
   Comma-separated levels.

   Type: ``String``

   Default: ``35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

