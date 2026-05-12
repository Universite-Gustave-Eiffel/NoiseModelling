.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Plot Exposition Distribution
============================

Overview
--------

Plot a graph displaying the distribution of a chosen field in a previously calculated Matsim agents noise exposition table. Will display a Graph Window on the server

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``expositionField`` — *Field containing noise exposition*
   Field containing noise exposition

   Type: ``String``

``expositionsTableName`` — *Name of the table containing the expositions*
   Name of the table containing the expositions
   The table must contain the following fields :
   PK, PERSON_ID, HOME_FACILITY, HOME_GEOM, WORK_FACILITY, WORK_GEOM, LAEQ, HOME_LAEQ, DIFF_LAEQ

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``otherExpositionField`` — *Other field containing noise exposition*
   Other field containing noise exposition

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

