.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Add Primary Key
===============

Add primary key column or constraint

Overview
--------

➡️ Add a Primary Key (🔑) column or add a Primary Key constraint to a column of a table.
It is necessary to add a Primary Key on one of the columns for the source and receiver tables before doing a calculation.  💡 If the table already has a Primary Key, it will remove the constraint before the operation.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pkName``
   Name of the column to be added, or for which the main key constraint will be added.  💡 Primary keys must contain UNIQUE values, and cannot contain NULL values

``tableName``
   Name of the table on which a primary key will be added

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
