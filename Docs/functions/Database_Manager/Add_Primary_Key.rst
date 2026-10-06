.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Add Primary Key
===============

Add primary key column or constraint

Overview
--------

➡️ Adds a Primary Key (🔑) column, or adds a Primary Key constraint to an existing column.
It is necessary to add a Primary Key on one of the columns for the source and receiver tables before doing a calculation.  💡 If the table already has a Primary Key, it will remove the constraint before the operation.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pkName`` — *Name of the column*
   Name of the column to be added, or for which the main key constraint will be added. 💡 Primary keys must contain UNIQUE values, and cannot contain NULL values

   Type: ``String``

``tableName`` — *Name of the table*
   Name of the table on which a primary key will be added

   Type: ``String``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

