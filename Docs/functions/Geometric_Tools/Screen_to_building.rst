.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Screen to building
==================

Convert screens to building format

Overview
--------

➡️ Convert the screens to the building format.
A width of 10 cm will be defined. If you also give a building table, this WPS script allows you to merge the two layers together.   Tables must be projected in a same metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.  ✅ The output table is called : BUILDINGS_SCREENS and contain: -  THE_GEOM : the 2D geometry of the created table (POLYGON or MULTIPOLYGON). -  HEIGHT : the height of the created polygons (FLOAT)

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableScreens`` — *Screens table name*
   Name of the Screens table.   The table must contain: -  THE_GEOM  : the 2D geometry of the screens (POLYGON or MULTIPOLYGON). -  HEIGHT  : the height of the screens (FLOAT)

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``tableBuilding`` — *Buildings table name*
   Name of the Buildings table.   The table must contain: -  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON). -  HEIGHT : the height of the building (FLOAT)

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

