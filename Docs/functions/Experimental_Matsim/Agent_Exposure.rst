.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Agent Exposure
==============

Calculate Mastim agents exposure

Overview
--------

Loads a Matsim plans.xml file and calculate agents noise exposure, based on previously claculated timesliced noisemap at receiver positions, linked with matsim activities (facilities)

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``plansFile``
   Path of the Matsim output_plans file  For example : /home/mastim/simulation_output/output_plans.xml.gz

``experiencedPlansFile``
   Path of the Matsim output_plans file  For example : /home/mastim/simulation_output/output_experienced_plans.xml.gz

``receiversTable``
   Table containing the receivers position
   The table must contain the following fields :
   PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES

``dataTable``
   Table containing the noise data
   The table must contain the following fields :
   PK, IDRECEIVER, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ000, HZ8000, PERIOD

``timeBinSize``
   This parameter dictates the time resolution of the resulting data
   The time information stored will be the starting time of the time bins
   For exemple with a timeBinSize of 3600, the data will be analysed using the following timeBins:
   0, 3600, 7200, ..., 79200, 82800

``outTableName``
   Name of the table you want to create from the file.
   The table will contain the following fields :
   PK, PERSON_ID, HOME_FACILITY, HOME_GEOM, WORK_FACILITY, WORK_GEOM, LAEQ, HOME_LAEQ, DIFF_LAEQ

Optional inputs
~~~~~~~~~~~~~~~

``personsCsvFile``
   Path of the Matsim output_persons csv file  For example : /home/mastim/simulation_output/output_persons.csv.gz

``SRID``
   Original projection identifier (also called SRID) of your tables.It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator

   Default: ``4326``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
