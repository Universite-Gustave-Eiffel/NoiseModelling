.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Traffic From Events
===================

Import traffic data from Mastim simultaion output folder

Overview
--------

Read Mastim events output file in order to get traffic NoiseModelling input

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``folder``
   Path of the Matsim output folder  For example : /home/mastim/simulation_output
   The folder must contain at least the following files:
   
   - output_network.xml.gz
   
   - output_allVehicles.xml.gz
   
   - output_events.xml.gz

Optional inputs
~~~~~~~~~~~~~~~

``timeBinSize``
   This parameter dictates the time resolution of the resulting data
   The time information stored will be the starting time of the time bins
   For exemple with a timeBinSize of 3600, the data will be analysed using the following timeBins:
   0, 3600, 7200, ..., 79200, 82800
   Default: 3600

``timeBinMin``
   The minimum of time bins in

   Default: ``0``

``timeBinMax``
   The maximum of time bins in

   Default: ``86400``

``populationFactor``
   Set the population factor of the MATSim simulation
   Must be a decimal number between 0 and 1
   Default: 1.0

``link2GeometryFile``
   The path of the pt2matsim CSV file generated when importing OSM network. Ignored if not set.
   The file must contain at least two columns :
   
   - The link ID
   
   - The WKT geometry

``SRID``
   Projection identifier (also called SRID) of the geometric data.It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator

   Default: ``4326``

``exportTraffic``
   Define if you want to output average speed and flow per vehicle category in an additional table
   Default: False

``skipUnused``
   Define if links with unused traffic should be omitted in the output table.
   Default: True

``outTableName``
   Name of the table you want to create.
   A table with this name will be created plus another with a "_LW" suffix
   For exemple if set to "MATSIM_ROADS (default value)":
   
   - the table MATSIM_ROADS, with the link ID and the geometry field
   
   - the table MATSIM_ROADS_LW, with the link ID and the traffic data

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
