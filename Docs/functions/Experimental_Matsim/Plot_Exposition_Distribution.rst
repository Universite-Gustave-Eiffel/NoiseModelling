.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Plot Exposition Distribution
============================

Overview
--------

Plot a graph displaying the distribution of a chosen field in a previously calculated Matsim agents noise exposition table. Will display a Graph Window on the server

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``expositionsTableName``
   Name of the table containing the expositions
   The table must contain the following fields :
   PK, PERSON_ID, HOME_FACILITY, HOME_GEOM, WORK_FACILITY, WORK_GEOM, LAEQ, HOME_LAEQ, DIFF_LAEQ

``expositionField``
   Field containing noise exposition

Optional inputs
~~~~~~~~~~~~~~~

``otherExpositionField``
   Other field containing noise exposition

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
