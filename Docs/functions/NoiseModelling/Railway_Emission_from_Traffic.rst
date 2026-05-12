.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Railway Emission from Traffic
=============================

Compute railway emission noise map from vehicule, traffic table AND section table.

Overview
--------

➡️ Compute Rail Emission Noise Map from Day, Evening and Night traffic flow rate and speed estimates (specific format, see input details).
✅ The output table is called LW_RAILWAY

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRailwayTraffic``
   Name of the Rail traffic table.
   This function recognize the following columns (* mandatory):
   
   * IDTRAFFIC* : A traffic identifier (PRIMARY KEY) (INTEGER)
   
   * IDSECTION* : A section identifier, refering to RAIL_SECTIONS table (INTEGER)
   
   * TRAINTYPE* : Type of vehicle, listed in the Rail_Train_SNCF_2021 file (mainly for french SNCF) (STRING)
   
   * TRAINSPD* : Maximum Train speed (in km/h) (DOUBLE)
   
   * TDAY, TEVENING and TNIGHT : Hourly average train count (6-18h)(18-22h)(22-6h) (INTEGER)

``tableRailwayTrack``
   Name of the Railway Track table.
   This function recognize the following columns (* mandatory):
   
   * IDSECTION* : A section identifier (PRIMARY KEY) (INTEGER)
   
   * NTRACK* : Number of tracks (INTEGER)
   
   * TRACKSPD* : Maximum speed on the section (in km/h) (DOUBLE)
   
   * TRANSFER : Track transfer function identifier, e.g. "SNCF5" or "EU7" (VARCHAR)
   
   * ROUGHNESS : Rail roughness identifier, e.g. "SNCF1" or "EU3" (VARCHAR)
   
   * IMPACT : Impact noise coefficient identifier, e.g. "SNCF1" or "EU1", empty for none (VARCHAR)
   
   * CURVATURE : Listed code describing the curvature of the section (INTEGER)
   
   * BRIDGE : Bridge transfer function identifier, e.g. "EU3", empty for none (VARCHAR)
   
   * TRACKSPD : Commercial speed on the section (in km/h) (DOUBLE)
   
   * ISTUNNEL : Indicates whether the section is a tunnel or not (0 = no / 1 = yes) (BOOLEAN)

Optional inputs
~~~~~~~~~~~~~~~

``vehicleDataFile``
   URL of the railway vehicle data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

``trainSetDataFile``
   URL of the railway train set data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

``railwayEmissionDataFile``
   URL of the railway emission data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
