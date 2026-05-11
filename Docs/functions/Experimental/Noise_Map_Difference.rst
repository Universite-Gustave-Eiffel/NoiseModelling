.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Noise Map Difference
====================

Map Difference

Overview
--------

➡️ Computes the difference between two noise maps

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``mainMapTable``
   Name of the table containing the primary noise map data.
   
   The table must contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

``secondMapTable``
   Name of the table containing the second noise map data.
   
   The table must contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

``outTable``
   Name of the table you want to create
   
   The table will contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

Optional inputs
~~~~~~~~~~~~~~~

``invert``
   Invert the substraction?
   
   * False (default) : Primary map - Second map
   
   * True : Second map - Primary map

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
