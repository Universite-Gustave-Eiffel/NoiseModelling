Enrich_DEM_with_rail
====================

Enrich a DEM with railway platform points.

Overview
--------

``Enrich_DEM_with_rail.groovy`` inserts altimetric points coming from railways into the input DEM.

It uses a railway platform scheme derived from the railway width column.

.. figure:: railway_plateform.png
   :align: center
   :alt: Railways platform

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Input DEM table to enrich.

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

   If not specified, the DEM SRID is used.

   Type: ``Integer``

``hRail``
   Railway platform height in meters.

   Default: ``0.5``

   Type: ``Double``

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

* It removes DEM points close to railway corridors before inserting railway platform points.
* It generates three buffered railway envelopes derived from the width field and inserts points at different heights.
* The final enriched DEM name is built from the input DEM plus the chosen suffix.

