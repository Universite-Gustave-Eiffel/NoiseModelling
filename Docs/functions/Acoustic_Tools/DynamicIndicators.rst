.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

DynamicIndicators
=================

Compute dynamic indicators

Overview
--------

Computes dynamic percentile indicators (L10, L50, L90) for each row in the table

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``columnName`` — *Column name*
   Column name on which to perform the calculation. (STRING)  For example : LEAQ

   Type: ``String``

``tableName`` — *Name of the table*
   Name of the table on which to perform the calculation. The table must contain multiple sound level values for a single receiver. The columns of the table should be named HZ63, HZ125,..., HZ8000 with an HZ prefix that can be changed. (STRING)  For example : RECEIVERS_LEVEL

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``outputTableName`` — *Name of the output table*
   Name of the output table

   Type: ``String``

   Default: ``tableName_DYN_IND``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

