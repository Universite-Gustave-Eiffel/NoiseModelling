.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

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

``sourceIndexFieldName`` — *Source index field name*
   The field name of the source index, will be translated into IDSOURCE

   Type: ``String``

``sourcePeriodFieldName`` — *Source period field name*
   The field name of the source period (ex. T), will be translated into PERIOD

   Type: ``String``

``tableSourceDynamic`` — *Source table name*
   Name of the Source table.    The source table have for the same index multiple periods, other columns can be any supported columns of noise level from emission or noise level from traffic

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``sourceEmissionTableName`` — *Source emission table name*
   The output table that contain for each source index, the period and other attributes of the source. Default is SOURCES_EMISSION. Can be used directly on noise_level_from_source or Noise_From_Attenuation_Matrix

   Type: ``String``

``sourceGeomTableName`` — *Source geometry table name*
   The output table that contain the distinct source index with the appropriate geometry. Default is SOURCES_GEOM

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

