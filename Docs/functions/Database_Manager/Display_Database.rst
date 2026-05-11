Display_Database
================

Display the list of database tables and optionally their attributes.

Overview
--------

``Display_Database.groovy`` displays the list of tables present in the database.

Optionally, it can also display the columns of each table.

To visualize the content of a table, the script description points users to ``Table_Visualization_Data``.

Arguments
---------

Optional inputs
~~~~~~~~~~~~~~~

``showColumns``
   Whether the columns of each table should also be displayed.

   A small key symbol is shown for columns that have a primary-key constraint.

   Type: ``Boolean``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It ignores the system tables ``SPATIAL_REF_SYS`` and ``GEOMETRY_COLUMNS``.
* When ``showColumns`` is enabled, it lists each column and marks geometry columns with their SRID and primary-key columns with a key symbol.
* If no user tables are found, it returns a formatted message indicating that the database is empty.

