.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Noise level from source
=======================

Computes the propagation from the sounds sources to the receivers

Overview
--------

➡️ Computes the propagation from the sounds sources to the receivers location using the noise emission table.
🌍 Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed. ✅ The output table are called:  RECEIVERS_LEVEL  The output table contain:

*  IDRECEIVER: receiver an identifier (INTEGER) linked to RECEIVERS table primary key

*  IDSOURCE: source identifier (INTEGER) linked to SOURCES_GEOM primary key. Only if Keep source id is checked.

*  PERIOD : Time period (VARCHAR) ex. L D E and DEN. Only if you provide emission power to sources or the atmospheric settings table.

*  THE_GEOM : the 3D geometry of the receivers with the Z as the altitude (POINTZ)

*  Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000 : 8 columns giving the sound level for each octave band (FLOAT)

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableBuilding`` — *Buildings table name*
   🏠 Name of the Buildings table The table must contain:
   
   *  THE_GEOM : the 2D geometry of the building (POLYGON or MULTIPOLYGON)
   
   *  HEIGHT : the height of the building (FLOAT)
   
   *  G : Optional, Wall absorption value if g is [0, 1] or wall surface impedance ([N.s.m-4] static air flow resistivity of material) if G is [20, 20000] (default is 0.1 if the column G does not exists) (FLOAT)

   Type: ``String``

``tableReceivers`` — *Receivers table name*
   Name of the Receivers table  The table must contain:
   
   *   PK  : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)
   
   *   THE_GEOM  : the 3D geometry of the sources (POINT, MULTIPOINT)
   
   💡 This table can be generated from the WPS Blocks in the "Receivers" folder

   Type: ``String``

``tableSources`` — *Sources geometry table name*
   Name of the Sources table (if only geometry is specified)  The table must contain (* mandatory):
   
   *   PK * : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)
   
   *   THE_GEOM * : the 3D geometry of the sources (POINT, MULTIPOINT, LINESTRING, MULTILINESTRING). According to CNOSSOS-EU, you need to set a height of 0.05 m for a road traffic emission
   
   *   HZD63, HZD125, HZD250, HZD500, HZD1000, HZD2000, HZD4000, HZD8000  : 8 columns giving the day emission sound level for each octave band (FLOAT)
   
   *   HZE  : 8 columns giving the evening emission sound level for each octave band (FLOAT)
   
   *   HZN  : 8 columns giving the night emission sound level for each octave band (FLOAT)
   
   *   YAW  : Source horizontal orientation in degrees. For points 0° North, 90° East. For lines 0° line direction, 90° right of the line direction.  (FLOAT)
   
   *   PITCH  : Source vertical orientation in degrees. 0° front, 90° top, -90° bottom. (FLOAT)
   
   *   ROLL  : Source roll in degrees (FLOAT)
   
   *   DIR_ID  : identifier of the directivity sphere from tableSourceDirectivity parameter or train directivity if not provided -> OMNIDIRECTIONAL(0), ROLLING(1), TRACTIONA(2), TRACTIONB(3), AERODYNAMICA(4), AERODYNAMICB(5), BRIDGE(6) (INTEGER)
   
   💡 This table can be generated from the WPS Block "Road_Emission_from_Traffic"

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``confDiffHorizontal`` — *Diffraction on horizontal edges*
   Compute or not the diffraction on horizontal edges

   Type: ``Boolean``

   Default: ``false``

``confDiffVertical`` — *Diffraction on vertical edges*
   Compute or not the diffraction on vertical edges. Following Directive 2015/996, enable this option for rail and industrial sources only

   Type: ``Boolean``

   Default: ``false``

``confExportSourceId`` — *Separate receiver level by source identifier*
   Keep source identifier in output in order to get noise contribution of each noise source. When only the source geometry is given, the attenuation between each pair of "source-receiver" points is specified (commonly referred to as the "attenuation matrix")

   Type: ``Boolean``

   Default: ``false``

``confFavourableOccurrencesDefault`` — *Probability of occurrences*
   Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one.
   
   .. figure:: acoustics_parameters_confFavorableOccurrences.png
      :align: center
      :alt: Noise level from source

   Type: ``String``

   Default: ``0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5``

``confHumidity`` — *Relative humidity*
   🌧 Humidity for noise propagation (%) [0,100]

   Type: ``Double``

   Default: ``70``

``confMaxError`` — *Max Error (dB)*
   Threshold for excluding negligible sound sources in calculations.This parameter is ignored if no emission level is specified or if you set it to 0 dB. This parameter have a great impact on computation time.

   Type: ``Double``

   Default: ``0.1``

``confMaxReflDist`` — *Maximum source-reflexion distance*
   Maximum search distance of walls / facades from the "Source-Receiver" segment, for the calculation of specular reflections (meters).
   
   .. figure:: acoustics_parameters_confMaxReflDist.png
      :align: center
      :alt: Noise level from source

   Type: ``Double``

   Default: ``50``

``confMaxSrcDist`` — *Maximum source-receiver distance*
   Maximum distance between source and receiver (FLOAT, in meters).
   
   .. figure:: acoustics_parameters_confMaxSrcDist.png
      :align: center
      :alt: Noise level from source

   Type: ``Double``

   Default: ``150``

``confRaysName`` — *Export scene*
   Save each mnt, buildings and propagation rays into the specified table (ex:RAYS) or file URL (ex: file:///Z:/dir/map.kml)  You can set a table name here in order to save all the rays computed by NoiseModelling.  The number of rays has been limited in this script in order to avoid memory exception.  🛠 If not provided, then do not keep rays

   Type: ``String``

``confReflOrder`` — *Order of reflexion*
   Maximum number of reflections to be taken into account (INTEGER).  🚨 Adding 1 order of reflexion can significantly increase the processing time.

   Type: ``Integer``

   Default: ``1``

``confTemperature`` — *Air temperature*
   🌡 Air temperature (°C)

   Type: ``Double``

   Default: ``15``

``confThreadNumber`` — *Thread number*
   Number of thread to use on the computer (INTEGER).  🛠

   Type: ``Integer``

   Default: ``0``

``frequencyFieldPrepend`` — *Frequency field name*
   Frequency field name prepend. Ex. for 1000 Hz frequency the default column name is HZ1000.

   Type: ``String``

   Default: ``HZ``

``paramWallAlpha`` — *Wall absorption coefficient*
   Wall absorption coefficient [0,1] (between ``0`` : "fully reflective" and ``1`` : "fully absorbent")

   Type: ``Double``

   Default: ``0.1``

``tableDEM`` — *DEM table name*
   Name of the Digital Elevation Model (DEM) table  The table must contain:
   
   *   THE_GEOM  : the 3D geometry of the sources (POINT, MULTIPOINT)
   
   💡 This table can be generated from the WPS Block "Import_Asc_File"

   Type: ``String``

``tableGroundAbs`` — *Ground absorption table name*
   Name of the surface/ground acoustic absorption table  The table must contain:
   
   *   THE_GEOM : the 2D geometry of the sources (POLYGON or MULTIPOLYGON)
   
   *   G : the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft)

   Type: ``String``

``tablePeriodAtmosphericSettings`` — *Atmospheric settings table name for each time period*
   Name of the Atmospheric settings table  The table must contain the following columns:
   
   *   PERIOD : time period (VARCHAR PRIMARY KEY)
   
   *   WINDROSE : probability of occurrences of favourable propagation conditions (ARRAY(16))
   
   *   TEMPERATURE : Temperature in celsius (FLOAT)
   
   *   PRESSURE : air pressure in pascal (FLOAT)
   
   *   HUMIDITY : air humidity in percentage (FLOAT)
   
   *   GDISC : choose between accept G discontinuity or not (BOOLEAN) default true
   
   *   PRIME2520 : choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false

   Type: ``String``

``tableSourceDirectivity`` — *Source directivity table name*
   Name of the emission directivity table  If not specified the default is train directivity of CNOSSOS-EU  The table must contain the following columns:
   
   *   DIR_ID : identifier of the directivity sphere (INTEGER)
   
   *   THETA : [-90;90] Vertical angle in degree. 0° front 90° top -90° bottom (FLOAT)
   
   *   PHI : [0;360] Horizontal angle in degree. 0° front 90° right (FLOAT)
   
   *   HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000 : attenuation levels in dB for each octave or third octave (FLOAT)

   Type: ``String``

``tableSourcesEmission`` — *Sources emission table name*
   Name of the Sources table (ex. SOURCES_EMISSION)  The table must contain:
   
   *  IDSOURCE * : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)
   
   *  PERIOD * : Time period, you will find this column on the output (VARCHAR)
   
   *   HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000  : Emission noise level in dB can be third-octave 50Hz to 10000Hz (FLOAT)

   Type: ``String``

Output
------

``result`` — *Created table*
   Name of the table containing the results of the computation. Can be used as input for another process.

   Type: ``String``

