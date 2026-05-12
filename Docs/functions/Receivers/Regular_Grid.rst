.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Regular Grid
============

Overview
--------

➡️ Computes a regular grid of receivers.
The receivers are spaced at a distance "delta" (Offset) in the Cartesian plane in meters.  The grid will be based on:

*  the BUILDINGS table extent (option by default)

*  OR a single Geometry "fence" (see "Extent filter" parameter).

✅ The output table is called RECEIVERS

.. figure:: regular_grid_output.png
   :align: center
   :alt: Regular grid output

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``fenceTableName``
   Using the bounding box of the given table name, define the envelope of the output grid:
   
   *  Extract the bounding box of the specified table,
   
   *  then create only receivers on the table bounding box.
   
   The given table must contain:
   
   *  THE_GEOM : any geometry type with the appropriate SRID

Optional inputs
~~~~~~~~~~~~~~~

``buildingTableName``
   Name of the Buildings table. Receivers inside buildings will be removed.The table must contain:
   
   *  THE_GEOM  : the 2D geometry of the building (POLYGON or MULTIPOLYGON)

``fence``
   Create receivers only in the provided polygon (fence)

``sourcesTableName``
   Keep only receivers at least at 1 meters of provided sources geometries  The given table must contain:
   
   *  THE_GEOM : any geometry type.

``delta``
   Offset in the Cartesian plane (in meters)

   Default: ``10``

``receiverstablename``
   Name of the output table. Do not write the name of a table that contains a space.

   Default: ``RECEIVERS``

``height``
   Height of receivers (in meter) (FLOAT)

   Default: ``4``

``outputTriangleTable``
   Output a triangle table in order to be used to generate iso contours with Create_Isosurface

Output
------

``result``
   Name of the table containing the results of the computation. Can be used as input for another process.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
