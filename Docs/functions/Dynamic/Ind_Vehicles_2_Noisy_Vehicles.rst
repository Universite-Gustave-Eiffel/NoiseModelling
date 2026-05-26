.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Ind Vehicles 2 Noisy Vehicles
=============================

Convert Individual Vehicles traffic to emission noise level and Snap them to the network point sources.

Overview
--------

Calculating dynamic road emissions based on vehicles trajectories and snap them to the network   The output table is called : SOURCES_EMISSION  and contain : -   IDSOURCE   : an identifier (INTEGER). -   PERIOD  : The TIMESTAMP iteration (STRING).-   HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000  : 8 columns giving the emission sound level for each octave band (FLOAT).

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableFormat`` — *Format of the individual Vehicles table*
   Format of the individual Vehicles table. Can be for the moment SUMO or Matsim. See in the code to understand the different format.

   Type: ``String``

``tableSourceGeom`` — *table of the source geometry*
   table of points source geometry, the output emission will be reattached to the index of this table according to the snap distance. Should be SOURCES_GEOM See Point_Source_From_Network to convert lines to points

   Type: ``String``

``tableVehicles`` — *table of the individual Vehicles*
   it should contain timestep, geometry (POINT), speed, acceleration, veh_type...

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``distance2snap`` — *Maximum distance to snap on the network point sources*
   Maximum distance to snap on the network point sources

   Type: ``Double``

``keepNoEmissionGeoms`` — *Keep source geometries without emission value*
   Do not delete source geometries that does not contain any emission value. Default to true, it reduce the computation time when evaluating the attenuation

   Type: ``Boolean``

   Default: ``true``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

