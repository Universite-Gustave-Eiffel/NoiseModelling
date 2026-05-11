.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

DynamicIndicators
=================

Compute dynamic indicators

Overview
--------

Compute dynamic indicators as L10, L90  The columns of the table should be named HZ63, HZ125,..., HZ8000 with an HZ prefix that can be changed.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``columnName``
   Column name on which to perform the calculation. (STRING)  For example : LEQA

``tableName``
   Name of the table on which to perform the calculation. The table must contain multiple sound level values for a single receiver. (STRING)  For example : RECEIVERS_LEVEL

Optional inputs
~~~~~~~~~~~~~~~

``outputTableName``
   Name of the output table default to tableName+_DYN_IND

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
