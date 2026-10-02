.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Atmospheric Template
====================

Generate default atmospheric settings from the PERIOD field of a noise emission table

Overview
--------

➡️ Generate default atmospherics settings from the PERIOD field of a noise emission table. It is used to export the result table to be edited and reimported to be used into Noise_level_from_source. This table make you able to change the temperature and other settings for each time period of the simulation

Arguments
---------

Optional inputs
~~~~~~~~~~~~~~~

``confDutchFraction`` — *Dutch favourable fraction*
   Use the Dutch formulas on calculating the ratio for favourable/homogenous propagation

   Type: ``Boolean``

``confFavourableOccurrencesDefault`` — *Default favourable occurrences*
   Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one.
   
   .. figure:: acoustics_parameters_confFavorableOccurrences.png
      :align: center
      :alt: Noise level from source
   
   . For Netherlands check confDutchFraction instead of using this parameter.

   Type: ``String``

   Default: ````

``confHumidity`` — *Relative humidity*
   🌧 Humidity for noise propagation (%) [0,100]

   Type: ``Double``

   Default: ``70``

``confTemperature`` — *Air temperature*
   🌡 Air temperature (°C)

   Type: ``Double``

   Default: ``15``

``tablePeriodAtmosphericSettings`` — *Output table name*
   Name of the Atmospheric settings table  The table will contain the following columns:
   
   *   PERIOD : time period (VARCHAR PRIMARY KEY)
   
   *   WINDROSE : Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one.
   
   .. figure:: acoustics_parameters_confFavorableOccurrences.png
      :align: center
      :alt: Noise level from source
   
   or DutchD, DutchE, DutchN for Netherlands
   
   *   TEMPERATURE : Temperature in celsius (FLOAT)
   
   *   PRESSURE : air pressure in pascal (FLOAT)
   
   *   HUMIDITY : air humidity in percentage (FLOAT)
   
   *   GDISC : choose between accept G discontinuity or not (BOOLEAN) default true
   
   *   PRIME2520 : choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false

   Type: ``String``

   Default: ``SOURCES_ATMOSPHERIC``

``tableSourcesEmission`` — *Sources emission table name*
   Name of the Sources table (ex. SOURCES_EMISSION)  The table must contain:
   
   *  IDSOURCE * : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)
   
   *  PERIOD * : Time period, you will find this column on the output (VARCHAR)

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

