.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Receivers From Activities Closest
=================================

Chose Closest Receivers For Matsim Activities

Overview
--------

Chose the closest receiver in a RECEIVERS table for every Mastim Activity in an ACTIVITIES table

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``activitiesTable``
   Name of the table containing the activities
   The table must contain the following fields :
   PK, FACILITY, THE_GEOM, TYPES

``receiversTable``
   Name of the table containing the receivers
   The table must contain the following fields :
   PK, THE_GEOM

``outTableName``
   Name of the table you want to create
   The table will contain the following fields :
   PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
