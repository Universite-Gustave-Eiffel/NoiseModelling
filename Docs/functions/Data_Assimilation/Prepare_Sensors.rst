.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Prepare Sensors
===============

Preparation of Sensor data

Overview
--------

Extracts sensor data for a given period and creates SQL tables

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``endDate`` — *End Time Stamp*
   The end timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S"

   Type: ``String``

``startDate`` — *Start Time Stamp*
   The start timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S"

   Type: ``String``

``targetSRID`` — *Target projection identifier*
   🌍 Target projection identifier (also called SRID) of your table.
   It should be an EPSG code, an integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).
   
   🚨 The target SRID must be in metric coordinates (e.g 2056 for Geneva).

   Type: ``Integer``

``trainingRatio`` — *Training data percentage*
   Training data as a percentage of total data

   Type: ``Float``

``workingFolder`` — *Working directory path with input files*
   Folder containing .csv files "device_mapping_sf", the .osm file and the folder "devices_data"

   Type: ``String``

Output
------

``result`` — *Sql tables output*
   Sql tables "SENSORS_MEASUREMENTS", "SENSORS_LOCATION", "SENSORS_MEASUREMENTS_TRAINING"

   Type: ``String``

