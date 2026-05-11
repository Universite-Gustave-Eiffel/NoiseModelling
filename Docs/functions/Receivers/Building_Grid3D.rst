Buildings Grid
===============

Overview
--------

Generates 3D receivers around building facades at multiple vertical levels. The number of levels depends on building height and the configured spacing between levels. The script creates a ``RECEIVERS`` table and can also distribute building population to receivers when a ``POP`` column is present.

.. image:: Building_Grid3D.png
   :alt: Building grid 3D output
   :width: 95%
   :align: center

Arguments
---------

``tableBuilding``
  Buildings table name. The table must contain ``THE_GEOM`` as building polygons or multipolygons and ``HEIGHT`` as building height in meters. An optional ``POP`` field can also be present.

``fence``
  Optional polygon geometry used to restrict receiver creation to a specific area.

``fenceTableName``
  Optional table name used to derive a bounding box filter from its ``THE_GEOM`` column.

``sourcesTableName``
  Optional sources table. Receivers closer than 1 meter to the provided source geometries are removed.

``delta``
  Optional spacing between receivers along facades, in meters. Default: ``10``.

``heightLevels``
  Optional vertical spacing between receiver levels, in meters. Default: ``2.5``.

``distance``
  Optional offset from the wall in meters. Default: ``2``.

Output
------

The script returns the created ``RECEIVERS`` table name. The output table stores 3D receiver geometries, a ``level`` field, and ``pk_building``. When population is available in the buildings table, a ``pop`` field is also added.

Function Signatures
-------------------

.. code-block:: groovy

   def exec(Connection connection, Map input)

Execution Notes
---------------

- The buildings table must have an integer primary key and a ``HEIGHT`` field.
- The script starts receiver elevations at 1.5 meters and adds new levels every ``heightLevels`` meters until the building height is covered.
- A direct ``fence`` geometry is reprojected to the buildings or sources SRID, while ``fenceTableName`` uses the table envelope directly.
- Receivers inside buildings are removed at the end of processing.
- If a ``POP`` field exists on the buildings table, the script distributes the population across the generated receivers.
