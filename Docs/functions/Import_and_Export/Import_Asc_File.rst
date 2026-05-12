.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Import Asc File
===============

Import Asc File.

Overview
--------

➡️ Import ESRI Ascii Raster file and convert into a Digital Elevation Model (DEM) compatible with NoiseModelling (X,Y,Z).
Valid file extensions : asc and asc.gz .  ✅ The output table is called: DEM and contain: - THE_GEOM: the 3D point cloud of the DEM (POINT)

.. figure:: import_asc_file.png
   :align: center
   :alt: Import asc file

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile`` — *Path of the ESRI Ascii Raster file*
   📂 Path of the ESRI Ascii Raster file you want to import, including its extension. Files can be gzip compressed.  For example: c:/home/receivers.asc or c:/home/receivers.asc.gz

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``downscale`` — *Skip pixels on each axis*
   Divide the number of rows and columns read by the following coefficient (FLOAT)

   Type: ``Integer``

   Default: ``1.0``

``fence`` — *Fence geometry*
   Create DEM table only in the provided polygon

   Type: ``Geometry``

``inputSRID`` — *Projection identifier*
   🌍 Original projection identifier (also called SRID) of the .asc files.  It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Pseudo-Mercator projection)

   Type: ``Integer``

   Default: ``4326``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

