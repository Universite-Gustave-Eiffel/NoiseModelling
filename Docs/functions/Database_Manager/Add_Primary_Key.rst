Add_Primary_Key
===============

Add a primary-key column or constraint.

Overview
--------

``Add_Primary_Key.groovy`` adds a primary-key column to a table, or adds a primary-key constraint to an existing column.

This is useful because source and receiver tables must have a primary key before running some calculations.

If the table already has a primary key, the script removes that constraint before applying the requested change.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pkName``
   Name of the column to add, or of the existing column that should receive the primary-key constraint.

   Primary-key values must be unique and cannot contain ``NULL`` values.

   Type: ``String``

``tableName``
   Name of the table on which a primary key will be added.

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It normalizes the table name and target column name to uppercase.
* If the table already has an integer primary key, the existing primary-key constraint is dropped first.
* If the requested column already exists, the script makes it ``INT NOT NULL`` and adds a primary-key constraint.
* Otherwise, it creates a new ``INT AUTO_INCREMENT PRIMARY KEY`` column with the requested name.

