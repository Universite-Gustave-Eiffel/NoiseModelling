.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Building Grid3D
===============

Buildings Grid

Overview
--------

➡️ Generates 3D receivers around the buildings and at different levels.
Main parameters:

* "Height between levels": coupled with the building height, allows to determine the number of levels,

* "Distance from wall": set the distance between the receivers and the building facades,

* "Distance between receivers": set the number of receivers around the buildings.

✅ The output table is called RECEIVERS

.. figure:: Building_Grid3D.png
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
   
   *  POP : building population to add in the receiver attribute (FLOAT) (Optionnal)

``delta``
   Distance between receivers (in the Cartesian plane - in meters) (FLOAT)

   Default: ``10``

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

``heightLevels``
   Height between each level of receivers (in meters) (FLOAT)

   Default: ``2.5``

``distance``
   Distance of receivers from the wall (in meters) (FLOAT)

   Default: ``2``

Output
------

``result``
   Name of the table containing the results of the computation. Can be used as input for another process.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
