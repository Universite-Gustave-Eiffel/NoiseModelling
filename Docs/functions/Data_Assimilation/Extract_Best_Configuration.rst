.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Extract Best Configuration
==========================

Extraction of the best configurations

Overview
--------

Extraction of the best maps, i.e. those that minimise the difference between the measured and simulated values, by calculating the minimum median values.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``noiseMapTable`` — *Noise map table*
   Table of "noiseMapTable" containing the noise maps after simulation

   Type: ``String``

``observationTable`` — *Measurement table*
   Table of "observationSensor" containing the training data Set

   Type: ``String``

``tempToleranceThreshold`` — *Temperature tolerance threshold*
   Temperature tolerance threshold used to filter and extract the best configurations

   Type: ``Double``

Output
------

``result`` — *Best Configuration Table*
   BEST_CONFIGURATION_FULL table created

   Type: ``String``

