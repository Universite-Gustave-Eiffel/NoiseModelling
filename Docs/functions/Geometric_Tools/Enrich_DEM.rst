.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

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

* Roads platform height (hRoad) (Optional). Default value = 0m

* Railroads right-of-way (railWidth): Name of column where the railroad right-of-way is stored (Mandatory)

* Rail platform height (hRail) (Optional). Default value = 0.5m

* Input SRID (inputSRID): SRID of the input tables (Optional)

* Output suffixe (outputSuffixe): Suffixe applied at the end of the resuling table name (Optional). If not specified, "ENRICHED" is applied

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

``inputDEM`` — *Input DEM table*
   Name of the input DEM table to be enriched

   Type: ``String``

``inputHydro`` — *Input hydrographic table*
   Name of the input hydrographic network table

   Type: ``String``

``inputOro`` — *Input orography table*
   Name of the input orography table

   Type: ``String``

``inputRail`` — *Input railways table*
   Name of the input railways table

   Type: ``String``

``inputRoad`` — *Input roads table*
   Name of the input roads table

   Type: ``String``

``railWidth`` — *Railways width*
   Name of column where the railways width is stored

   Type: ``String``

``roadWidth`` — *Road width*
   Name of column where the road width is stored

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``hRail`` — *Railways platform height*
   Railways platform height (in meters) (Optional)

   Type: ``double``

   Default: ``0.5``

``hRoad`` — *Roads platform height*
   Roads platform height (in meters) (Optional)

   Type: ``Double``

   Default: ``0``

``inputSRID`` — *Input SRID*
   🌍 SRID of the input tables.  🛠 If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied

   Type: ``Integer``

``outputSuffixe`` — *Output suffix*
   Suffix applied at the end of the resulting table name

   Type: ``String``

   Default: ``ENRICHED``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

