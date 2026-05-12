.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Enrich DEM with lines
=====================

Overview
--------

➡️ Insert altimetric points coming from linestring input layers into the input DEM.
This script works with two input layers:

* Digital Elevation Model (DEM) to be enriched

* A linestring layer (e.g: hydrographic network, ...) in which coordinates have a Z dimension

And three optional parameters:

* Input SRID (inputSRID): SRID of the input tables

* Source (source): Text indicating the source of the linestring layer. Can be useful to distinguish the points in the resulting DEM . If not specified, "LINESTRING" is applied

* Output suffix (outputsuffix): suffix applied at the end of the resuling table name. If not specified, "ENRICHED" is applied

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM`` — *Input DEM table*
   Name of the input DEM table to be enriched

   Type: ``String``

``inputLine`` — *Input Linestring table*
   Name of the input Linestring table

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID`` — *Input SRID*
   🌍 SRID of the input tables.  🛠 If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied

   Type: ``Integer``

``outputsuffix`` — *Output suffix*
   Suffix applied at the end of the resulting table name

   Type: ``String``

   Default: ``ENRICHED``

``source`` — *Source*
   Text indicating the source of the linestring layer (Optional)

   Type: ``String``

   Default: ``LINESTRING``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

