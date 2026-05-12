.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Agent Exposure
==============

Calculate Matsim agents exposure

Overview
--------

Loads a Matsim plans.xml file and calculate agents noise exposure, based on previously calculated timesliced noisemap at receiver positions, linked with matsim activities (facilities)

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``dataTable`` — *Table containing the noise data*
   Table containing the noise data
   The table must contain the following fields :
   PK, IDRECEIVER, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ000, HZ8000, PERIOD

   Type: ``String``

``experiencedPlansFile`` — *Path of the Matsim output_experienced_plans file*
   Path of the Matsim output_plans file  For example : /home/matsim/simulation_output/output_experienced_plans.xml.gz

   Type: ``String``

``outTableName`` — *Name of created table*
   Name of the table you want to create from the file.
   The table will contain the following fields :
   PK, PERSON_ID, HOME_FACILITY, HOME_GEOM, WORK_FACILITY, WORK_GEOM, LAEQ, HOME_LAEQ, DIFF_LAEQ

   Type: ``String``

``plansFile`` — *Path of the Matsim output_plans file*
   Path of the Matsim output_plans file  For example : /home/matsim/simulation_output/output_plans.xml.gz

   Type: ``String``

``receiversTable`` — *Table containing the receivers position*
   Table containing the receivers position
   The table must contain the following fields :
   PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES

   Type: ``String``

``timeBinSize`` — *The size of time bins in seconds.*
   This parameter dictates the time resolution of the resulting data
   The time information stored will be the starting time of the time bins
   For exemple with a timeBinSize of 3600, the data will be analysed using the following timeBins:
   0, 3600, 7200, ..., 79200, 82800

   Type: ``Integer``

Optional inputs
~~~~~~~~~~~~~~~

``SRID`` — *Projection identifier*
   Original projection identifier (also called SRID) of your tables.It should be an EPSG code; an integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).

   Type: ``Integer``

``personsCsvFile``
   Path of the Matsim output_persons csv file  For example : /home/matsim/simulation_output/output_persons.csv.gz

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

