.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

GenerateAtmosphericSettingsTemplate
===================================

Generate default atmospherics settings from the PERIOD field of a noise emission table

Overview
--------

➡️ Generate default atmospherics settings from the PERIOD field of a noise emission table. It is used to export the result table to be edited and reimported to be used into Noise_level_from_source or Noise_level_from_traffic. This table make you able to change the temperature and other settings for each time period of the simulation

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableSourcesEmission``
   Name of the Sources table (ex. SOURCES_EMISSION)  The table must contain:
   
   *  IDSOURCE * : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)
   
   *  PERIOD * : Time period, you will find this column on the output (VARCHAR)

Optional inputs
~~~~~~~~~~~~~~~

``tablePeriodAtmosphericSettings``
   Name of the Atmospheric settings table  The table will contain the following columns:
   
   *   PERIOD : time period (VARCHAR PRIMARY KEY)
   
   *   WINDROSE : probability of occurrences of favourable propagation conditions (ARRAY(16))
   
   *   TEMPERATURE : Temperature in celsius (FLOAT)
   
   *   PRESSURE : air pressure in pascal (FLOAT)
   
   *   HUMIDITY : air humidity in percentage (FLOAT)
   
   *   GDISC : choose between accept G discontinuity or not (BOOLEAN) default true
   
   *   PRIME2520 : choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false
   
   Default to SOURCES_ATMOSPHERIC

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
