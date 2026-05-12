.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

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

``mainMapTable`` — *Primary map table name*
   Name of the table containing the primary noise map data.
   
   The table must contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

   Type: ``String``

``outTable`` — *Name of created table*
   Name of the table you want to create
   
   The table will contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

   Type: ``String``

``secondMapTable`` — *Secondary map table name*
   Name of the table containing the second noise map data.
   
   The table must contain the following columns:
   PK, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, LAEQ, LEQ

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``invert`` — *Invert the substraction ?*
   Invert the substraction?
   
   * False (default) : Primary map - Second map
   
   * True : Second map - Primary map

   Type: ``Boolean``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

