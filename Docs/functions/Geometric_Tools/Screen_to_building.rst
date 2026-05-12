.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Screen to building
==================

Convert screens to building format.

Overview
--------

➡️ Convert the screens to the building format.
A width of 10 cm will be defined. If you also give a building table, this WPS script allows you to merge the two layers together.   Tables must be projected in a same metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.  ✅ The output table is called : BUILDINGS_SCREENS and contain: -  THE_GEOM : the 2D geometry of the created table (POLYGON or MULTIPOLYGON). -  HEIGHT : the height of the created polygons (FLOAT)

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableScreens``
   Name of the Screens table.   The table must contain: -  THE_GEOM  : the 2D geometry of the screens (POLYGON or MULTIPOLYGON). -  HEIGHT  : the height of the screens (FLOAT)

Optional inputs
~~~~~~~~~~~~~~~

``tableBuilding``
   Name of the Buildings table.   The table must contain: -  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON). -  HEIGHT : the height of the building (FLOAT)

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
