Clean_Buildings_Table
=====================

Clean a buildings table.

Overview
--------

``Clean_Buildings_Table.groovy`` cleans a buildings table by avoiding overlapping areas and invalid building polygons.

The input table is erased and replaced by the cleaned version.

NoiseModelling propagation code does not handle intersecting building polygons well.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Buildings table name.

   The table must:

   * use a metric coordinate system
   * contain ``THE_GEOM`` as building geometry
   * contain ``HEIGHT`` as building height

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It validates that the buildings table has a metric SRID.
* It runs geometry cleanup steps including precision reduction, topology-preserving simplification, and ``ST_MAKEVALID``.
* It detects overlapping buildings, truncates intersections, and rebuilds the original table.
* If a ``POP`` column exists, it is preserved in the cleaned output.

