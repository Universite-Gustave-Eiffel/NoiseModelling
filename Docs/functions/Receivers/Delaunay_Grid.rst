Delaunay Grid
=============

Overview
--------

Computes a receiver grid based on Delaunay triangulation. The computation can use either a provided fence geometry or the extent of a fence table, and it creates a ``RECEIVERS`` table. When triangle export is enabled, a ``TRIANGLES`` table is also created.

.. image:: delaunay_grid_output.png
   :alt: Delaunay grid output
   :width: 95%
   :align: center

Arguments
---------

``fenceTableName``
  Optional table name used to derive the receiver computation extent from its geometry envelope.

``tableBuilding``
  Buildings table name. The table must contain ``THE_GEOM`` as building polygons or multipolygons.

``fence``
  Optional polygon geometry used to restrict the computation area.

``sourcesTableName``
  Sources table name. This parameter is required by the script.

``maxCellDist``
  Optional maximum cell size in meters used to split the domain into sub-domains for triangulation. Default: ``600``.

``skipCellNoSourcesMinimalDistance``
  Optional distance threshold in meters. If provided, a sub-domain is skipped when no sources are found within that distance.

``roadWidth``
  Optional minimum distance to source geometries in meters. Receivers closer than this distance are not created. Default: ``2``.

``buildingBuffer``
  Optional minimum distance to building footprints in meters. Default: ``2``.

``maxArea``
  Optional maximum triangle area in square meters. Smaller values create more receivers. Default: ``2500``.

``height``
  Optional receiver height relative to the ground, in meters. Default: ``4``.

``outputTableName``
  Optional output table name. Default: ``RECEIVERS``.

``isoSurfaceInBuildings``
  Optional boolean. If enabled, isosurfaces may be created over building areas. Default: ``false``.

``fenceNegativeBuffer``
  Optional negative buffer in meters applied to the computation envelope. Default: ``0``.

``exportTrianglesGeometries``
  Optional boolean. If enabled, triangle geometries are exported to the ``TRIANGLES`` table. Default: ``false``.

Output
------

The script returns the created receiver table name. It always creates a receiver grid table and can also create a ``TRIANGLES`` table when triangle export is enabled.

Function Signatures
-------------------

.. code-block:: groovy

   def ensureSpatialIndex(Connection connection, String table)
   def exec(Connection connection, Map input)

Execution Notes
---------------

- ``sourcesTableName`` is mandatory for this script.
- The computation requires a metric SRID. The script raises an error if the sources and buildings tables are not in a metric projection or if no SRID is available.
- The script uses ``DelaunayReceiversMaker`` to build the receiver mesh and can subdivide the domain to limit memory usage.
- A direct ``fence`` geometry is reprojected when needed, while ``fenceTableName`` uses the table envelope.
- Spatial indexes are created on output tables if they do not already exist.
