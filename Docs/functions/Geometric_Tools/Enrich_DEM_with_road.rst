Enrich_DEM_with_road
====================

Enrich a DEM with road platform points.

Overview
--------

``Enrich_DEM_with_road.groovy`` inserts altimetric points coming from roads into the input DEM.

It uses road widths to build buffered road platforms and insert derived points into a new DEM table.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Input DEM table to enrich.

   Type: ``String``

``inputRoad``
   Input roads table.

   Type: ``String``

``roadWidth``
   Name of the column storing road width.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   SRID of the input tables.

   If not specified, the DEM SRID is used.

   Type: ``Integer``

``hRoad``
   Roads platform height in meters.

   Default: ``0``

   Type: ``Double``

``outputSuffix``
   Suffix applied to the resulting table name.

   Default: ``ENRICHED``

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes two functions:

* ``exec(Connection connection, input)``
* ``parseScript(String sqlInstructions, Sql sql, ProgressVisitor progressVisitor, Logger logger)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It removes DEM points close to roads before inserting buffered road-platform points.
* It computes a fallback half-width of ``1.5`` meters when the road width is missing or too small.
* The final output table name is built from the input DEM plus the chosen suffix.

