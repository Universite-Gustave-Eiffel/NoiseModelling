Buildings Grid
===============

Overview
--------

Generates facade receivers around building footprints, positioned 2 meters from the walls by default and placed at a fixed receiver height. The script creates a ``RECEIVERS`` table and stores the source building primary key in ``build_pk``. If the buildings table contains a ``POP`` field, the population is distributed across the generated receivers.

.. image:: building_grid_output.png
   :alt: Building grid output
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

``height``
  Optional receiver height in meters. Default: ``4``.

``distance``
  Optional offset from the wall in meters. Default: ``2``.

Output
------

The script returns the created ``RECEIVERS`` table name. The output table contains receiver geometries and a ``build_pk`` field, plus ``pop`` when the input buildings table includes a population column.

Function Signatures
-------------------

.. code-block:: groovy

   def exec(Connection connection, Map input)
   double splitLineStringIntoPoints(LineString geom, double segmentSizeConstraint, List<Coordinate> pts)

Execution Notes
---------------

- The buildings table must have an integer primary key and a ``HEIGHT`` field.
- The script can use either a direct ``fence`` geometry or the envelope of ``fenceTableName`` as a spatial filter.
- Receiver lines are built from buffered building outlines, truncated where taller neighboring buildings intersect them, then converted to regularly spaced points.
- If a ``POP`` field exists on the buildings table, the script distributes the building population evenly across its generated receivers.
- Temporary tables are created during processing and dropped before completion.
