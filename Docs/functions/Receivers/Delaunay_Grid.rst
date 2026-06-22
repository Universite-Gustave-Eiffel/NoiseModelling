.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Delaunay Grid
=============

Overview
--------

➡️ Computes a Delaunay grid of receivers.
The grid will be based on:

*  the BUILDINGS table extent (option by default)

*  OR a single Geometry "fence" (Extent filter).

✅ Two tables are returned:

*  RECEIVERS

*  TRIANGLES

.. figure:: delaunay_grid_output.png
   :align: center
   :alt: Delaunay grid output

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``sourcesTableName`` — *Sources table name*
   Name of the Road table.
   Receivers will not be created on the specified road width

   Type: ``String``

``tableBuilding`` — *Buildings table name*
   Name of the Buildings table.
   The table must contain:
   
   *   THE_GEOM  : the 2D geometry of the building (POLYGON or MULTIPOLYGON)

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``buildingBuffer`` — *Minimum distance to buildings (m)*
   Do not add receivers closer than this distance to buildings (in meters)

   Type: ``Double``

   Default: ``2``

``exportTrianglesGeometries`` — *In the triangles table, export triangles geometries*
   If enabled, the TRIANGLES table will contain the geometry of each triangle

   Type: ``Boolean``

   Default: ``false``

``fence`` — *Extent filter*
   Create receivers only in the provided polygon (fence)

   Type: ``Geometry``

``fenceNegativeBuffer`` — *Negative buffer*
   Reduce the fence(parameter, or sound sources and buildings extent) used to generate receivers positions. You should set here the maximum propagation distance (in meters) (FLOAT)

   Type: ``Double``

   Default: ``0``

``fenceTableName`` — *Fence table name*
   Use the extent of a geometry table (e.g., from a shapefile) to limit receiver area

   Type: ``String``

``height`` — *Height*
   Receiver height relative to the ground (in meters) (FLOAT)

   Type: ``Double``

   Default: ``4``

``isoSurfaceInBuildings`` — *Create IsoSurfaces over buildings*
   If enabled, isosurfaces will be visible at the location of buildings

   Type: ``Boolean``

   Default: ``false``

``maxArea`` — *Maximum Area*
   Set Maximum Area (in m2) (FLOAT). No triangles larger than provided area will be created.Smaller area will create more receivers

   Type: ``Double``

   Default: ``2500``

``maxCellDist`` — *Maximum cell size*
   Maximum distance used to split the domain into sub-domains (in meters) (FLOAT).
   In a logic of optimization of processing times, it allows to limit the number of objects (buildings, roads, …) stored in memory during the Delaunay triangulation

   Type: ``Double``

   Default: ``600``

``outputTableName`` — *Name of output table*
   Name of the output table. Do not write the name of a table that contains a space

   Type: ``String``

   Default: ``RECEIVERS``

``outputTableNameTriangles`` — *Name of triangles output table*
   Name of the triangles output table.

   Type: ``String``

   Default: ``TRIANGLES``

``roadWidth`` — *Road width*
   Set Road Width (in meters) (FLOAT). No receivers closer than road width distance will be created.  You can set 0 m if you don't want to insert roads in the output but still want to skip cells without sources using the 'Skip cell no sources minimal distance' parameter

   Type: ``Double``

   Default: ``2``

``skipCellNoSourcesMinimalDistance`` — *Skip cell no sources minimal distance*
   If provided, a sub-domain will not be computed if no sources geometries are near x meters from the sub-domain area

   Type: ``Double``

Output
------

``result`` — *Created table*
   Name of the table containing the results of the computation. Can be used as input for another process.

   Type: ``String``

