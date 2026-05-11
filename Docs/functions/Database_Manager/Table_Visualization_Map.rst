Table_Visualization_Map
=======================

Display a table on a map.

Overview
--------

``Table_Visualization_Map.groovy`` displays a table containing a geometry column on a map.

Technically, it groups all geometries from the table and returns them in WKT OGC format.

The script warns that very large tables may block the treatment.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the table to display.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   Original projection identifier, or SRID, of the table.

   Coordinates are transformed from the specified SRID to WGS84 ``EPSG:4326``.

   This input is optional because some formats already include projection metadata and some imported files may not contain geometry attributes.

   Default: ``4326``

   Type: ``Integer``

Output
------

``result``
   Output geometry in WKT OGC form.

   Type: ``Geometry``

Function Signatures
-------------------

The script exposes two functions:

* ``exec(Connection connection, Map input)``
* ``asWKT(Geometry geometry)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It normalizes the table name to uppercase.
* It checks that the table contains at least one geometry column.
* If the table already has an SRID and it differs from a user-provided ``inputSRID``, the script raises an exception.
* When the table SRID is known, that SRID is used instead of the default input value.
* The geometries are transformed to ``EPSG:4326`` and accumulated into a single returned geometry.

