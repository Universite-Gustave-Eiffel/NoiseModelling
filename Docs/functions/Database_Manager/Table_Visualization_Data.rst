.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Table Visualization Data
========================

Display first rows of a query result.

Overview
--------

➡️ Display the content of a SQL query result.
You can provide either a table name or a complete SELECT SQL query. Using "linesNumber" parameter, you can choose the number of lines to display  🚨 Be careful, this treatment can be very long if the query returns many rows.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``linesNumber``
   Number of rows you want to display. This parameter is ignored if your SQL query already contains a LIMIT clause.

``tableName``
   Table name or SQL SELECT query (e.g., mytable or SELECT * FROM mytable)

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
