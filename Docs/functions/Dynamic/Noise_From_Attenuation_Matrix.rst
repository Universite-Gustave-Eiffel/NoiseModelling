.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Noise From Attenuation Matrix
=============================

Noise Map From Attenuation Matrix

Overview
--------

Noise Map From Attenuation Matrix.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``lwTable``
   LW(PERIOD) ex. SOURCES_EMISSION
   The table must contain the following fields :
   IDSOURCE, PERIOD, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000
   IDSOURCE link to primary key of attenuation table and PERIOD a varchar

``attenuationTable``
   Attenuation Matrix Table name, Obtained from the Noise_level_from_source script with "confExportSourceId" enabled. Should be RECEIVERS_LEVEL
   The table must contain the following fields :
   IDRECEIVER, IDSOURCE, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000

``outputTable``
   outputTable

Optional inputs
~~~~~~~~~~~~~~~

``lwTable_sourceId``
   LW(PERIOD) source index field. Default is IDSOURCE

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
