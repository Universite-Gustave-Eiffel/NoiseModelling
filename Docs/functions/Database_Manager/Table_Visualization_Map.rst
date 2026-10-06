.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Table Visualization Map
=======================

Display a table on a map.

Overview
--------

➡️ Display a table containing a geometric column on a map 🗺
Technically, it groups all the geometries of a table and returns them in WKT OGC format.   🚨 Be careful, this treatment can be blocked if the table is too large.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName`` — *Name of the table*
   Name of the table you want to display.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID`` — *Projection identifier*
   🌍 Original projection identifier (also called SRID) of your table. It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). (INTEGER)  All coordinates will be projected from the specified EPSG to WGS84 coordinates.  This entry is optional because many formats already include the projection and you can also import files without geometry attributes.

   Type: ``Integer``

Output
------

``result`` — *Result output geometry*
   This is the output geometry in WKT OGC format

   Type: ``Geometry``

