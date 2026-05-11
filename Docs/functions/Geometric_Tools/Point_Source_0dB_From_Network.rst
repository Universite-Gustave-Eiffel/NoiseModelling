.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Point Source 0dB From Network
=============================

Create 0db Source From Roads

Overview
--------

➡️ Creates a SOURCE table from a ROAD table.
The SOURCE table can then be used in the Noise_level_from_source WPS block with the "confExportSourceId" set to true. The Noise_level_from_source output will contain a list of "source-receiver" attenuation matrix independent of the source absolute noise power levels.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads``
   Name of the Roads table.
   
   Must contain at least:- PK: identifier with a Primary Key constraint- THE_GEOM: geometric column

``gridStep``
   Distance between location of vehicle along the network in

   Default: ``10``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
