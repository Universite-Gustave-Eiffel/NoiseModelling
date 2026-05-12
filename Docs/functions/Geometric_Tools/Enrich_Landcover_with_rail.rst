.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Enrich Landcover with rail
==========================

Enrich Landcover with railways

Overview
--------

➡️ Insert rail ground surfaces into the input LANDCOVER.
This script works with two input layers:

* Landcover to be enriched

* Railways

And four parameters:

* Railroads right-of-way (railWidth): Name of column where the railroad right-of-way is stored (Mandatory)

* Rail platform height (hRail): Railways platform height (Optional). Default value = 0.5m

* Input SRID (inputSRID): SRID of the input tables (Optional)

* Output suffix (outputsuffix): suffix applied at the end of the resuling table name (Optional). If not specified, "ENRICHED" is applied

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

``gColumn`` — *G column*
   Ground absorption coefficient (G) column name

   Type: ``String``

``inputLandcover`` — *Input landcover table*
   Name of the input landcover table

   Type: ``String``

``inputRail`` — *Input railways table*
   Name of the input railways table

   Type: ``String``

``railWidth`` — *Railways width*
   Name of column where the railways width is stored

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

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

