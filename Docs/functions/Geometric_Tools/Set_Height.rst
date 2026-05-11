Set_Height
==========

Set or update geometry heights.

Overview
--------

``Set_Height.groovy`` updates the geometry of a table by adding a Z value either from a fixed height or from a height column in the table.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Name of the table whose geometry height will be modified.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``height``
   Static height value in meters.

   Type: ``Double``

``heightColumn``
   Name of the column containing heights to use.

   Type: ``String``

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

* It detects the geometry column name from the target table.
* When a static ``height`` is provided, it updates all geometries to that Z value.
* When ``heightColumn`` is provided, it updates geometry Z values from the referenced column.
* In both cases, it updates the geometry metadata to mark the geometry as 3D.

