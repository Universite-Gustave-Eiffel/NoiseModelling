.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/autodoc/GenerateFunctionsDocs.java

Combine Noise Maps
==================

Combine two noise maps

Overview
--------

➡️ Combines two receiver level tables (same receiver grid) by computing either their difference or their energetic sum.
Each table must contain IDRECEIVER, THE_GEOM and at least one common level column (HZ63, HZ125, ... or LAEQ, LEQA, LEQ).

Difference : firstTable - secondTable (in dB).
Energetic sum : 10 * log10(10^(firstTable/10) + 10^(secondTable/10)).

Periods are never aggregated : the output contains one row per (IDRECEIVER, PERIOD).

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``firstTable`` — *First table*
   Name of the first receiver level table. (STRING) Example : RECEIVERS_LEVEL_BEFORE

   Type: ``String``

``secondTable`` — *Second table*
   Name of the second receiver level table. It must use the same receiver grid as the first table. (STRING) Example : RECEIVERS_LEVEL_AFTER

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``frequencyPrefix`` — *Frequency prefix*
   Optional prefix of the frequency band columns (e.g. HZ). Leave empty to automatically detect the common frequency columns.

   Type: ``String``

   Default: (empty string)

``operation`` — *Operation*
   Operation to apply :
   
   * difference : first table - second table (default)
   
   * energetic_sum : energetic sum of the two tables

   Type: ``String``

   Default: ``difference``

``outputTable`` — *Output table*
   Optional name of the created table. If empty, NOISE_MAP_DIFFERENCE or NOISE_MAP_ENERGETIC_SUM is used.

   Type: ``String``

   Default: (empty string)

``periodColumn`` — *Period column*
   Optional name of the period column (e.g. PERIOD). Leave empty to automatically use PERIOD, then TIMESTRING. If no period column is found, the join is done on IDRECEIVER only.

   Type: ``String``

   Default: (empty string)

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

