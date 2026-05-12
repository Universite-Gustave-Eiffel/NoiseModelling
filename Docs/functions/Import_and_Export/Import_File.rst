.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Import File
===========

Overview
--------

➡️ Import file into the database.
Valid file extensions: csv, dbf, geojson, json, geojson.gz, gpx, osm.bz2, osm.gz, osm, shp, tsv

.. figure:: import_file.png
   :align: center
   :alt: Import file

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile``
   📂 Path of the file you want to import, including its extension. For example: c:/home/buildings.geojson

``ifTableExists``
   What to do if a table with the same name already exists ?

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   🌍 Original projection identifier (also called SRID) of your table.  It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Pseudo-Mercator projection).  This entry is optional because many formats already include the projection and you can also import files without geometry attributes. If the table is geometric and if this parameter is not filled and:- the file has a .prj file associated: the SRID is deduced from the .prj - the file has no .prj file associated: we apply the WGS84 (EPSG:4326) code

   Default: ``4326``

``tableName``
   Name of the table you want to create from the file.

   Default: ``it will take the name of the file without its extension``

Output
------

``outputTable``
   Name of the created table

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
