.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Enrich DEM with lines
=====================

Overview
--------

➡️ Insert altimetric points coming from linestring input layers into the input DEM.
This script works with two input layers:

* Digital Elevation Model (DEM) to be enriched

* A linestring layer (e.g: hydrographic network, ...) in which coordinates have a Z dimension

And three optionnal parameters:

* Input SRID (inputSRID): SRID of the input tables

* Source (source): Text indicating the source of the linestring layer. Can be useful to distinguish the points in the resulting DEM . If not specified, "LINESTRING" is applied

* Output suffixe (outputSuffixe): Suffixe applied at the end of the resuling table name. If not specified, "ENRICHED" is applied

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Name of the input DEM table to be enriched

``inputLine``
   Name of the input Linestring table

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   🌍 SRID of the input tables.  🛠 If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied

``source``
   Text indicating the source of the linestring layer (Optionnal)  🛠 If not specified, "LINESTRING" is applied

``outputSuffixe``
   Suffixe applied at the end of the resuling table name  🛠 If not specified, "ENRICHED" is applied

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
