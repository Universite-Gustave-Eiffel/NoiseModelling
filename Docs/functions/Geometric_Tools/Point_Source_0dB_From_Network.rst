.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Point Source 0dB From Network
=============================

Create 0 dB Source From Roads

Overview
--------

➡️ Creates a SOURCE table from a ROAD table.
The SOURCE table can then be used in the Noise_level_from_source WPS block with the "confExportSourceId" set to true. The Noise_level_from_source output will contain a list of "source-receiver" attenuation matrix independent of the source absolute noise power levels.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads`` — *Input table name*
   Name of the Roads table.
   
   Must contain at least:- PK: identifier with a Primary Key constraint- THE_GEOM: geometric column

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``gridStep``
   Distance between location of vehicle along the network in meters.

   Type: ``Integer``

   Default: ``10``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

