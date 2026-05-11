Simplify_Geometries
===================

Simplify geometries in a table.

Overview
--------

``Simplify_Geometries.groovy`` simplifies geometries in a selected table using the Douglas-Peucker algorithm.

The input table geometries are updated in place.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the table whose geometries will be simplified.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``distanceTolerance``
   Tolerance distance used for simplification.

   Default: ``1``

   Type: ``Double``

``preserveTopology``
   Whether topology should be preserved.

   Default: ``false``

   Type: ``Boolean``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It validates that the table has a geometry column and a metric SRID.
* If ``preserveTopology`` is true, it uses ``ST_SimplifyPreserveTopology``.
* Otherwise, it uses ``ST_Simplify`` and logs that invalid geometries may result.

