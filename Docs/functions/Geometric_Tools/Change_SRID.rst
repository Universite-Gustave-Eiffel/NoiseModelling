.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Change SRID
===========

Change or set SRID

Overview
--------

➡️ Assign a new Spatial Reference Identifier (SRID) to the specified table
🚨 If the table: - has already an associated SRID: the new SRID is applied to the table and a reprojection of geometries is done, - has no associated SRID: the new SRID is applied to the table but without doing a reprojection of geometries.

.. figure:: change_SRID.png
   :align: center
   :alt: Change SRID

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``newSRID`` — *Projection identifier*
   🌍 New projection identifier (also called SRID) of your table.  It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Pseudo-Mercator projection)

   Type: ``Integer``

``tableName`` — *Name of the table*
   Name of the table you want to change the SRID (and reproject if the table has already a SRID)

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

