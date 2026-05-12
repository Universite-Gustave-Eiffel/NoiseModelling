.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Enrich DEM
==========

Overview
--------

➡️ Insert altimetric points coming from input layers into the input DEM.
This script works with five input layers:

* Digital Elevation Model (DEM) to be enriched

* Orographic lines

* Hydrograpic network

* Roads

* Railways

And six parameters:

* Road width (roadWidth): Name of column where the road width is stored (Mandatory)

* Roads platform height (hRoad) (Optionnal). Default value = 0m

* Railroads right-of-way (railWidth): Name of column where the railroad right-of-way is stored (Mandatory)

* Rail platform height (hRail) (Optionnal). Default value = 0.5m

* Input SRID (inputSRID): SRID of the input tables (Optionnal)

* Output suffixe (outputSuffixe): Suffixe applied at the end of the resuling table name (Optionnal). If not specified, "ENRICHED" is applied

In the schema below, orange points will be inserted into the DEM. d2, d3 and d4 are deduced from the information provided in the parameter railWidth, using the following formula:

* d2 = (railWidth - 5.5)/2

* d3 = (railWidth - 4)/2

* d4 = (railWidth)/2

.. figure:: railway_plateform.png
   :align: center
   :alt: Railways platform

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Name of the input DEM table to be enriched

``inputOro``
   Name of the input orography table

``inputHydro``
   Name of the input hydrographic network table

``inputRoad``
   Name of the input roads table

``roadWidth``
   Name of column where the road width is stored

``inputRail``
   Name of the input railways table

``railWidth``
   Name of column where the railways width is stored

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   🌍 SRID of the input tables.  🛠 If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied

``hRoad``
   Roads platform height (in meters) (Optionnal)  🛠 Default value = 0

``hRail``
   Railways platform height (in meters) (Optionnal) 🛠 Default value = 0.5

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
