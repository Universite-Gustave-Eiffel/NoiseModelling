.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Import Activities
=================

Import Matsim "facilities" file

Overview
--------

Import Matsim "facilities" file containing agents activities location.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``facilitiesPath``
   Path of the Matsim facilities file

``outTableName``
   Name of the table you want to create
   The table will contain the following fields :
   PK, FACILITY, THE_GEOM, TYPES, BUILDING_ID

Optional inputs
~~~~~~~~~~~~~~~

``SRID``
   Original projection identifier (also called SRID) of your table.It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator

   Default: ``4326``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
