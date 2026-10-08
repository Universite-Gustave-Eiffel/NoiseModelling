.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/autodoc/GenerateFunctionsDocs.java

Copy Table with Select
======================

Copy a table with a select

Overview
--------

➡️ Copy a (subset) of table to a new table.
This script copies a table with a `WHERE` query to a new table. It can be used to make a quick selection of receiver points, or select a specific set of sources. The selection is based on either a set of IDs, or a WKT string. When the WKT string is used, it selects all the elements WITHIN the WKT string.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName`` — *Table name*
   🌍 Name of the table to copy.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``copyID`` — *ID(s) of row to be copied*
   Comma separated list of IDs from the source table to be copied to the target table.

   Type: ``String``

``wktString`` — *WKT string*
   WKT string of geometry (polygon) that encloses features to be copied. It should have the same SRID as the source table.Make sure to use double quote (") around the WKT string.

   Type: ``String``

Output
------

``outputTable`` — *Name of the created table*
   Name of the created table

   Type: ``String``

