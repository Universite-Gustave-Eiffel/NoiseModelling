.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Building Grid
=============

Buildings Grid

Overview
--------

➡️ Generates receivers, 2m around the building facades, at a given height.
✅ The output table is called RECEIVERS and contain a field build_pk corresponding to the primary key of the buildings table

.. figure:: building_grid_output.png
   :align: center
   :alt: Building grid output

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableBuilding``
   Name of the Buildings table.
   The table must contain:
   
   *  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON)
   
   *  HEIGHT : the height of the building (in meter) (FLOAT)
   
   *  POP : (optional field) building population to add in the receiver attribute (FLOAT)

Optional inputs
~~~~~~~~~~~~~~~

``fence``
   Create receivers only in the provided polygon (fence)

``fenceTableName``
   Filter receivers, using the bounding box of the given table name:
   
   *  Extract the bounding box of the specified table,
   
   *  then create only receivers on the table bounding box.
   
   The given table must contain:
   
   *  THE_GEOM : any geometry type.

``sourcesTableName``
   Keep only receivers that are at least 1 meter from the provided source geometries.The source geometries table must contain:
   
   *  THE_GEOM : any geometry type.

``delta``
   Distance between receivers (in the Cartesian plane - in meter) (FLOAT)

   Default: ``10``

``height``
   Height of receivers (in meter) (FLOAT)

   Default: ``4``

``distance``
   Distance of receivers from the wall in meters (FLOAT)

   Default: ``2``

Output
------

``result``
   Name of the table containing the results of the computation. Can be used as input for another process.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
