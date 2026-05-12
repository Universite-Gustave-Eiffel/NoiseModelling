.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Import Activities
=================

Import Matsim "facilities" file

Overview
--------

containing agents activities location.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``facilitiesPath`` — *Path of the Matsim facilities file*
   Path of the Matsim facilities file

   Type: ``String``

``outTableName`` — *Name of created table*
   Name of the table you want to create
   The table will contain the following fields :
   PK, FACILITY, THE_GEOM, TYPES, BUILDING_ID

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``SRID`` — *Projection identifier*
   Original projection identifier (also called SRID) of your table.It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).

   Type: ``Integer``

   Default: ``4326``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

