Enrich_DEM_with_lines
=====================

Enrich a DEM with Z-enabled linestring data.

Overview
--------

``Enrich_DEM_with_lines.groovy`` inserts altimetric points from a linestring layer into a DEM.

It works with:

* a DEM to enrich
* a linestring layer whose coordinates already carry Z values

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``inputDEM``
   Input DEM table to enrich.

   Type: ``String``

``inputLine``
   Input linestring table.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   SRID of the input tables.

   If not specified, the DEM SRID is used.

   Type: ``Integer``

``source``
   Label written into the output ``SOURCE`` field to identify the origin of inserted points.

   Default: ``LINESTRING``

   Type: ``String``

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

* It preserves an existing ``SOURCE`` field from the DEM if present, otherwise it creates one.
* It densifies the linestring layer and projects Z values from the source lines onto inserted DEM points.
* The final enriched table name is based on the input DEM plus the requested suffix.

