.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/autodoc/GenerateFunctionsDocs.java

Railway Emission from Traffic
=============================

Compute railway emission lines sources from vehicle, traffic table AND section table.

Overview
--------

➡️ Compute railway emission lines sources from Day, Evening and Night traffic flow rate and speed estimates (specific format, see input details).
✅ The output table is called LW_RAILWAY and will contain the columns pk_section, the_geom, dir_id, gs, hrail, cref

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRailwayTrack`` — *RailWay Track table name*
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
   
   * COMSPD : Commercial speed on the section (in km/h) (DOUBLE)
   
   * PLATFORM : Railway platform identifier, listed in the RailwayPlatforms.json file, e.g. "DEFAULT", "SNCF" or "SNCF_LGV" (VARCHAR, optional). If absent or empty, the DEFAULT platform is used. This input only affects emission and propagation calculations; it does not modify the site topography or its land cover. To update those, use the Enrich Landcover with railways and Enrich DEM with railways blocks
   
   * GS : Ground factor between the rails from 0 (hard) to 1 (soft) (DOUBLE, optional). Overrides the platform g3 value. If absent or null, the selected platform g3 is used (1 for DEFAULT)
   
   * ISTUNNEL : Indicates whether the section is a tunnel or not (0 = no / 1 = yes) (BOOLEAN)

   Type: ``String``

``tableRailwayTraffic`` — *Railway traffic table name*
   Name of the Rail traffic table.
   This function recognize the following columns (* mandatory):
   
   * IDTRAFFIC* : A traffic identifier (PRIMARY KEY) (INTEGER)
   
   * IDSECTION* : A section identifier, referring to RAIL_SECTIONS table (INTEGER)
   
   * TRAINTYPE* : Type of vehicle, listed in the Rail_Train_SNCF_2021 file (mainly for french SNCF) (STRING)
   
   * TRAINSPD* : Maximum Train speed (in km/h) (DOUBLE)
   
   * TDAY, TEVENING and TNIGHT : Hourly average train count (6-18h)(18-22h)(22-6h) (INTEGER)

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``railwayEmissionDataFile`` — *Railway emission data file*
   URL of the railway emission data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

   Type: ``String``

``railwayPlatformDataFile`` — *Railway platform data file*
   URL of the railway platform data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

   Type: ``String``

``trainSetDataFile`` — *Railway train set data file*
   URL of the railway train set data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

   Type: ``String``

``vehicleDataFile`` — *Railway vehicle data file*
   URL of the railway vehicle data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

