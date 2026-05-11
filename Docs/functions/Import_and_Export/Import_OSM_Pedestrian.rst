Import_OSM_Pedestrian
=====================

Import pedestrian-oriented tables from OSM.

Overview
--------

``Import_OSM_Pedestrian.groovy`` converts ``.osm``, ``.osm.gz``, or ``.osm.pbf`` files into NoiseModelling pedestrian-oriented input tables.

It creates tables including:

* ``BUILDINGS``
* ``PEDESTRIAN_WAYS``
* ``PEDESTRIAN_POIS``
* ``GROUND``
* ``PEDESTRIAN_AREA``

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile``
   Path of the OSM file, including extension.

   Supported extensions are ``.osm``, ``.osm.gz``, and ``.osm.pbf``.

   Type: ``String``

``targetSRID``
   Target projection identifier of the created tables.

   It must be metric.

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It reads the OSM file and extracts buildings, pedestrian ways, pedestrian POIs, and ground areas.
* It builds intermediate pedestrian-area tables and then derives a final ``PEDESTRIAN_AREA`` table.
* It creates spatial indexes on the generated geometry tables.

