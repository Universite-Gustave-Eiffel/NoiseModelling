Screen_to_building
==================

Convert screens to building format.

Overview
--------

``Screen_to_building.groovy`` converts screen geometries into building-style polygons and can optionally merge them with an existing buildings table.

The output table is ``BUILDINGS_SCREENS`` and contains:

* ``THE_GEOM``: polygon or multipolygon geometry
* ``HEIGHT``: height of the created polygons

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableScreens``
   Screens table name.

   The table must contain:

   * ``THE_GEOM``: screen geometry
   * ``HEIGHT``: screen height

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``tableBuilding``
   Buildings table name.

   If provided, the script merges the buffered screens with this building layer.

   The table must contain:

   * ``THE_GEOM``: building geometry
   * ``HEIGHT``: building height

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It requires screens and buildings to share the same metric SRID when both tables are used.
* It truncates intersecting screens and removes parts too close to buildings.
* It converts screens into polygons using a fixed buffer width of ``0.1`` meters.
* It creates ``BUILDINGS_SCREENS`` and adds a spatial index and an auto-increment primary key.

