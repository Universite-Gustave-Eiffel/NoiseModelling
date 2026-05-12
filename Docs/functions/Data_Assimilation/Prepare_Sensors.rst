.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Prepare Sensors
===============

Preparation of Sensor data

Overview
--------

Extraction of sensor data for a given period and creation of sql tables

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``startDate``
   the start timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S"

``endDate``
   the end timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S"

``trainingRatio``
   Training data as a percentage of total data

``workingFolder``
   Folder containing csv files "device_mapping_sf", the osm file and the folder "devices_data"

``targetSRID``
   🌍 Target projection identifier (also called SRID) of your table.
   It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).
   
   🚨 The target SRID must be in metric coordinates example 2056 for Geneva.

Output
------

``result``
   Sql tables "SENSORS_MEASUREMENTS", "SENSORS_LOCATION", "SENSORS_MEASUREMENTS_TRAINING"

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
