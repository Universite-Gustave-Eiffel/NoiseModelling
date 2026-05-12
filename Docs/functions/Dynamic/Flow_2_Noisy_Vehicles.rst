.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Flow 2 Noisy Vehicles
=====================

From Road traffic flows to noisy individual vehicles

Overview
--------

Calculating individual vehicle position and noise_level based on average traffic flows.   A first output table is called : SOURCES_GEOM  which is needed to compute the Noise Attenuation Matrixand contain : -   IDSOURCE   : an identifier (INTEGER, PRIMARY KEY). -   ROAD_ID   : id link to the road segment (INTEGER). -   THE_GEOM  : the 3D geometry of the sources (POINT).     The output table is called : SOURCES_EMISSION  and contain : -   PK   : an identifier (INTEGER, PRIMARY KEY). -   IDSOURCE   : link to the source point (INTEGER). -   PERIOD   : The TIMESTAMP iteration (VARCHAR).-   HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000  : 8 columns giving the instantaneous emission sound level for each octave band (FLOAT).

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads``
   Name of the Roads table.
   The table shall contain : -  PK  : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)
   -  LV_D  : Hourly average light and heavy vehicle count (DOUBLE)
   -  HGV_D  :  Hourly average heavy vehicle count (DOUBLE)
   -  LV_SPD_D  :  Hourly average light vehicle speed (DOUBLE)
   -  HGV_SPD_D  :  Hourly average heavy vehicle speed  (DOUBLE)
   -  PVMT  :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)   This table can be generated from the WPS Block 'Import_OSM'. .

``method``
   Two methods are available :  - PROBA : Probabilistic representation of vehicle appearances for each time step (quicker, but sacrifices temporal coherence) Aumond, P., Jacquesson, L., & Can, A. (2018). Probabilistic modeling framework for multisource sound mapping. Applied Acoustics, 139, 34-43. . - TNP : Simplified vehicle movements (slower, but maintaining temporal coherence) De Coensel, B.; Brown, A.L.; Tomerini, D. A road traffic noise pattern simulation model that includes distributions of vehicle sound power levels. Appl. Acoust. 2016, 111, 170–178. .

``timestep``
   Number of iterations. Timestep in sec.

``gridStep``
   Distance between location of vehicle along the network in meters.

``duration``
   Number of seconds to compute (INTEGER).

Output
------

``result``
   Name of the generated table. Can be used as the input of other process.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
