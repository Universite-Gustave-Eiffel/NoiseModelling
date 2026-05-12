.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Receivers From Activities Random
================================

Choose a random receivers for Matsim activities

Overview
--------

Choose the closest building for every Mastim activity in an ACTIVITIES table, and then chose a random receiver previously generated around this building.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``activitiesTable`` — *Name of the table containing the activities*
   Name of the table containing the activities
   The table must contain the following fields :
   PK, FACILITY, THE_GEOM, TYPES

   Type: ``String``

``buildingsTable`` — *Name of the table containing the buildings*
   Name of the table containing the buildings
   The table must contain the following fields :
   PK, THE_GEOM

   Type: ``String``

``outTableName`` — *Name of created table*
   Name of the table you want to create
   The table will contain the following fields :
   PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES, BUILD_PK

   Type: ``String``

``receiversTable`` — *Name of the table containing the receivers*
   Name of the table containing the receivers
   The table must contain the following fields :
   PK, THE_GEOM, BUILD_PK

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``randomSeed`` — *Random seed*
   Random seed

   Type: ``Integer``

   Default: ``1234``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

