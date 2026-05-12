.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Split Sources Period
====================

Aggregate by source index

Overview
--------

Split a single table with duplicated geometry and source identifier into SOURCES_GEOM and SOURCES_EMISSION tables

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableSourceDynamic``
   Name of the Source table.    The source table have for the same index multiple periods, other columns can be any supported columns of noise level from emission or noise level from traffic

``sourceIndexFieldName``
   The field name of the source index, will be translated into IDSOURCE

``sourcePeriodFieldName``
   The field name of the source period (ex. T), will be translated into PERIOD

Optional inputs
~~~~~~~~~~~~~~~

``sourceGeomTableName``
   The output table that contain the distinct source index with the appropriate geometry. Default is SOURCES_GEOM

``sourceEmissionTableName``
   The output table that contain for each source index, the period and other attributes of the source. Default is SOURCES_EMISSION. Can be used directly on noise_level_from_source or Noise_From_Attenuation_Matrix

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
