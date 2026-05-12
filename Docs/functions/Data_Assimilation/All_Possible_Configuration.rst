.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

All Possible Configuration
==========================

all configurations

Overview
--------

process to generate all configurations.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``trafficValues``
   list of variation values in % for traffic like [0.01,1.0, 2.0,3,4]

``temperatureValues``
   List of temperature values for the road traffic emission

Output
------

``result``
   A sql table named ALL_CONFIGURATIONS

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
