.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Noise From Attenuation Matrix
=============================

Noise Map From Attenuation Matrix

Overview
--------



Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``attenuationTable`` — *Attenuation Matrix Table name*
   Attenuation Matrix Table name, Obtained from the Noise_level_from_source script with "confExportSourceId" enabled. Should be RECEIVERS_LEVEL
   The table must contain the following fields :
   IDRECEIVER, IDSOURCE, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000

   Type: ``String``

``lwTable`` — *LW(PERIOD)*
   LW(PERIOD) ex. SOURCES_EMISSION
   The table must contain the following fields :
   IDSOURCE, PERIOD, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000
   IDSOURCE link to primary key of attenuation table and PERIOD a varchar

   Type: ``String``

``outputTable`` — *outputTable Matrix Table name*
   outputTable

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``lwTable_sourceId`` — *LW(PERIOD) source index field*
   LW(PERIOD) source index field. Default is IDSOURCE

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

