.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

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

``tableVehicles``
   it should contain timestep, geometry (POINT), speed, acceleration, veh_type...

``tableSourceGeom``
   table of points source geometry, the output emission will be reattached to the index of this table according to the snap distance. Should be SOURCES_GEOM See Point_Source_From_Network to convert lines to points

``tableFormat``
   Format of the individual Vehicles table. Can be for the moment SUMO or Matsim. See in the code to understand the different format.

Optional inputs
~~~~~~~~~~~~~~~

``distance2snap``
   Maximum distance to snap on the network point sources

``keepNoEmissionGeoms``
   Do not delete source geometries that does not contain any emission value. Default to true, it reduce the computation time when evaluating the attenuation

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
