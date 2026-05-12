.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Receivers From Activities Random
================================

Chose a Random Receivers For Matsim Activities

Overview
--------

Chose the closest building for every Mastim Activity in an ACTIVITIES table, and then chose a random receiver previously generated around this building.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``activitiesTable``
   Name of the table containing the activities
   The table must contain the following fields :
   PK, FACILITY, THE_GEOM, TYPES

``buildingsTable``
   Name of the table containing the buildings
   The table must contain the following fields :
   PK, THE_GEOM

``receiversTable``
   Name of the table containing the receivers
   The table must contain the following fields :
   PK, THE_GEOM, BUILD_PK

``outTableName``
   Name of the table you want to create
   The table will contain the following fields :
   PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES, BUILD_PK

Optional inputs
~~~~~~~~~~~~~~~

``randomSeed``
   Random seed, default: 1234

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
