.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

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

``observationTable``
   table of observationSensor containing the training data Set

``noiseMapTable``
   table of noiseMapTable containing the noise maps after simulation

``tempToleranceThreshold``
   temperature tolerance threshold to extract the best configuration

Output
------

``result``
   BEST_CONFIGURATION_FULL table created

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
