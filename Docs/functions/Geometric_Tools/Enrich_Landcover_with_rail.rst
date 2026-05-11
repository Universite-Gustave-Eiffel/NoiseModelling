Enrich_Landcover_with_rail
==========================

Enrich landcover with railway ground surfaces.

Overview
--------

``Enrich_Landcover_with_rail.groovy`` inserts railway-related ground surfaces into an input landcover table.

It rebuilds the landcover polygons by combining existing ground absorption classes with railway platform zones.

.. figure:: ../../wps_images/railway_plateform.png
   :align: center
   :alt: Railways platform

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputLandcover``
   Input landcover table.

   Type: ``String``

``gColumn``
   Ground absorption coefficient column name.

   Type: ``String``

``inputRail``
   Input railways table.

   Type: ``String``

``railWidth``
   Name of the column storing railway width.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   SRID of the input tables.

   If not specified, the landcover SRID is used.

   Type: ``Integer``

``outputSuffixe``
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

* It initializes a platform-definition table used to assign ground absorption classes around railways.
* It buffers the railway geometry into several rings and assigns different ``G`` values to each ring.
* It removes the overlapping parts from existing landcover classes and unions everything into a rebuilt output table.

