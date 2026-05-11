Drop_a_Table
============

Remove a table from the database.

Overview
--------

``Drop_a_Table.groovy`` removes a selected table from the database.

This operation is explicitly marked for cautious use.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableToDrop``
   Name of the table to drop.

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

* It normalizes the requested table name to uppercase.
* It ignores the system tables ``SPATIAL_REF_SYS`` and ``GEOMETRY_COLUMNS``.
* It searches the existing user tables and drops the matching one if found.
* If the table is not found, it returns a warning message instead.

