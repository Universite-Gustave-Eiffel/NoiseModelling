.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Enrich DEM with road
====================

Enrich DEM with roads

Overview
--------

➡️ Insert altimetric points coming from roads into the input DEM.
This script works with two input layers:

* Digital Elevation Model (DEM) to be enriched

* Roads

And four parameters:

* Roads right-of-way (roadWidth): Name of column where the road right-of-way is stored (Mandatory)

* Road platform height (hRoad): Roads platform height (Optional). Default value = 0.0m

* Input SRID (inputSRID): SRID of the input tables (Optional)

* Output suffix (outputSuffix): Suffix applied at the end of the resulting table name (Optional). If not specified, "ENRICHED" is applied

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Name of the input DEM table to be enriched

``inputRoad``
   Name of the input roads table

``roadWidth``
   Name of column where the road width is stored

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   🌍 SRID of the input tables.  🛠 If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied

``hRoad``
   Roads platform height (in meters) (Optional)   🛠 Default value = 0

``outputSuffix``
   Suffix applied at the end of the resulting table name  🛠 If not specified, "ENRICHED" is applied

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
