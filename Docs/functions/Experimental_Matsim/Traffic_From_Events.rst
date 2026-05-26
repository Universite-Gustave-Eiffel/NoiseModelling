.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Traffic From Events
===================

Import traffic data from Matsim simulation output folder

Overview
--------

Read Matsim events output file in order to get traffic NoiseModelling input

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``folder`` — *Path of the Matsim output folder*
   Path of the Matsim output folder  For example : /home/matsim/simulation_output
   The folder must contain at least the following files:
   
   - output_network.xml.gz
   
   - output_allVehicles.xml.gz
   
   - output_events.xml.gz

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``SRID`` — *Projection identifier*
   Projection identifier (also called SRID) of the geometric data.It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).

   Type: ``Integer``

``exportTraffic`` — *Export additionnal traffic data ?*
   Define if you want to output average speed and flow per vehicle category in an additional table

   Type: ``Boolean``

   Default: ``false``

``link2GeometryFile`` — *Network CSV file*
   The path of the pt2matsim CSV file generated when importing OSM network. Ignored if not set.
   The file must contain at least two columns :
   
   - The link ID
   
   - The WKT geometry

   Type: ``String``

``outTableName`` — *Output table name*
   Name of the table you want to create.
   A table with this name will be created plus another with a "_LW" suffix
   For exemple if set to "MATSIM_ROADS (default value)":
   
   - the table MATSIM_ROADS, with the link ID and the geometry field
   
   - the table MATSIM_ROADS_LW, with the link ID and the traffic data

   Type: ``String``

``populationFactor`` — *Population Factor*
   Set the population factor of the MATSim simulation
   Must be a decimal number between 0 and 1

   Type: ``Double``

   Default: ``1.0``

``skipUnused`` — *Skip unused links ?*
   Define if links with unused traffic should be omitted in the output table.

   Type: ``Boolean``

   Default: ``true``

``timeBinMax`` — *The maximum of time bins in seconds*
   The maximum of time bins in seconds,

   Type: ``Integer``

   Default: ``86400``

``timeBinMin`` — *The minimum of time bins in seconds*
   The minimum of time bins in seconds

   Type: ``Integer``

   Default: ``0``

``timeBinSize`` — *The size of time bins in seconds.*
   This parameter dictates the time resolution of the resulting data
   The time information stored will be the starting time of the time bins
   For exemple with a timeBinSize of 3600, the data will be analysed using the following timeBins:
   0, 3600, 7200, ..., 79200, 82800

   Type: ``Integer``

   Default: ``3600``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

