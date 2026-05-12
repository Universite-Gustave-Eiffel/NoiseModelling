.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Import OSM Pedestrian
=====================

Import Pedestrian tables from OSM

Overview
--------

➡️ Convert .osm, .osm.gz or .osm.pbf file into NoiseModelling input tables.

The following output tables will be created:
-  BUILDINGS : a table containing the buildings
💡 The user can choose to avoid creating some of these tables by checking the dedicated boxes

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile`` — *Path of the OSM file*
   📂 Path of the OSM file, including its extension (.osm, .osm.gz or .osm.pbf).
   For example: c:/home/area.osm.pbf

   Type: ``String``

``targetSRID`` — *Target projection identifier*
   🌍 Target projection identifier (also called SRID) of your table.
   It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).
   
   ❗ The target SRID must be in metric coordinates.

   Type: ``Integer``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

