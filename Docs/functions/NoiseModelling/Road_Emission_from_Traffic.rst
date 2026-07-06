.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Road Emission from Traffic
==========================

Compute road emission noise map from road table.

Overview
--------

➡️ Compute Road Emission Noise Map from Day Evening Night traffic flow rate and speed estimates (specific format, see input details).
✅ The output table is called: LW_ROADS

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads`` — *Roads table name*
   Name of the Roads table.
   If you provide the PERIOD field you do not need to provide the fields with the extension  _D _E _N. This function recognize the following columns (* mandatory) :
   
   *   PK  : If there is a primary key defined, it will be copied with the same name and set as a primary for the output table
   
   *  IDSOURCE  : an identifier, if present will be copied as is. It is expected if you will use LW_ROADS as SOURCES_EMISSION in the Noise_Level_From_Source script input (INTEGER)
   
   *  PERIOD  : Any text that could be time period ex. D, E, N, DEN (Varchar), if present will be copied as is
   
   *  LV  : Hourly average light vehicle count (DOUBLE)
   
   *  MV  : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (DOUBLE)
   
   *  HGV  : Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (DOUBLE)
   
   *  WAV  : Hourly average mopeds, tricycles or quads ≤ 50 cc count (DOUBLE)
   
   *  WBV  : Hourly average motorcycles, tricycles or quads > 50 cc count (DOUBLE)
   
   *  LV_SPD  : Hourly average light vehicle speed (DOUBLE)
   
   *  MV_SPD  : Hourly average medium heavy vehicles speed (DOUBLE)
   
   *  HGV_SPD  : Hourly average heavy duty vehicles speed (DOUBLE)
   
   *  WAV_SPD  : Hourly average mopeds, tricycles or quads ≤ 50 cc speed (DOUBLE)
   
   *  WBV_SPD  : Hourly average motorcycles, tricycles or quads > 50 cc speed (DOUBLE)
   
   *  LV_D LV_E LV_N  : Hourly average light vehicle count (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  MV_D MV_E MV_N  : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  HGV_D  HGV_E  HGV_N  : Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  WAV_D  WAV_E  WAV_N  : Hourly average mopeds, tricycles or quads ≤ 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  WBV_D  WBV_E  WBV_N  : Hourly average motorcycles, tricycles or quads > 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  LV_SPD_D  LV_SPD_E LV_SPD_N  : Hourly average light vehicle speed (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  MV_SPD_D  MV_SPD_E MV_SPD_N  : Hourly average medium heavy vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  HGV_SPD_D  HGV_SPD_E  HGV_SPD_N  : Hourly average heavy duty vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  WAV_SPD_D  WAV_SPD_E  WAV_SPD_N  : Hourly average mopeds, tricycles or quads ≤ 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  WBV_SPD_D  WBV_SPD_E  WBV_SPD_N  : Hourly average motorcycles, tricycles or quads > 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  TEMP  : Hourly average air temperature (DOUBLE)
   
   *  TEMP_D  TEMP_E  TEMP_N  : Hourly average air temperature (6-18h)(18-22h)(22-6h) (DOUBLE)
   
   *  PVMT  : CNOSSOS road pavement identifier (ex: NL05)(default NL08) (VARCHAR)
   
   *  TS_STUD  : A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres (0-12) (DOUBLE)
   
   *  PM_STUD  : Average proportion of vehicles equipped with studded tyres during TS_STUD period (0-1) (DOUBLE)
   
   *  JUNC_DIST  : Distance to junction in meters (DOUBLE)
   
   *  JUNC_TYPE  : Type of junction (k=0 none, k = 1 for a crossing with traffic lights ; k = 2 for a roundabout) (INTEGER)
   
   *  SLOPE  : Slope (in %) of the road section. If the field is not filled in, the LINESTRING z-values will be used to calculate the slope and the traffic direction (way field) will be force to 3 (bidirectional). (DOUBLE)
   
   *  WAY  : Define the way of the road section. 1 = one way road section and the traffic goes in the same way that the slope definition you have used, 2 = one way road section and the traffic goes in the inverse way that the slope definition you have used, 3 = bi-directional traffic flow, the flow is split into two components and correct half for uphill and half for downhill (INTEGER)
   
   This table can be generated from the WPS Block 'Import_OSM'. .

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``coefficientVersion`` — *Coefficient version*
   🌧 Cnossos coefficient version  (1 = 2015, 2 = 2020)

   Type: ``Double``

   Default: ``2``

``outputTable`` — *Output table name*
   🛠 Name of the output table. If the table already exists, it will be dropped and replaced by the new one.

   Type: ``String``

   Default: ``LW_ROADS``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

