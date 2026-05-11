Regular Grid
============

Overview
--------

Computes a regular receiver grid with a fixed spacing in the Cartesian plane. The grid is generated inside a provided fence geometry or the bounding box of a table, then filtered against buildings and sources. The script creates a ``RECEIVERS`` table and can optionally generate a ``TRIANGLES`` table for isosurface workflows.

.. image:: regular_grid_output.png
   :alt: Regular grid output
   :width: 95%
   :align: center

Arguments
---------

``buildingTableName``
  Optional buildings table name. Receivers inside buildings are removed. The table must contain ``THE_GEOM``.

``fence``
  Optional polygon geometry used to define the computation extent.

``fenceTableName``
  Optional table name used to derive the output envelope from its ``THE_GEOM`` bounding box.

``sourcesTableName``
  Optional sources table. Receivers closer than 1 meter to the provided source geometries are removed.

``delta``
  Optional receiver spacing in meters. Default: ``10``.

``receiverstablename``
  Optional output table name. Default: ``RECEIVERS``.

``height``
  Optional receiver height in meters. Default: ``4``.

``outputTriangleTable``
  Optional boolean. If enabled, a ``TRIANGLES`` table is created for later use with ``Create_Isosurface``.

Output
------

The script returns the created receiver table name. When ``outputTriangleTable`` is enabled, it also creates a ``TRIANGLES`` table containing triangle geometries and receiver references.

Function Signatures
-------------------

.. code-block:: groovy

   def exec(connection, Map input)

Execution Notes
---------------

- Either ``fence`` or ``fenceTableName`` must be provided, otherwise the script raises an error.
- The script uses ``ST_MakeGridPoints`` to generate a regular point lattice and stores row and column identifiers before adding a primary key.
- A direct ``fence`` geometry is reprojected to the detected SRID, while ``fenceTableName`` uses the table envelope.
- Receivers inside buildings and receivers closer than 1 meter to sources are removed after grid creation.
- When triangle export is enabled, the script builds a ``TRIANGLES`` table by connecting neighboring grid points into cell triangles.
