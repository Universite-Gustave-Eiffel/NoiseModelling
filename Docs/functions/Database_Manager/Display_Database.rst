.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Display Database
================

Display the list of tables (and their attributes).

Overview
--------

➡️ Displays the list of tables that are in the database.
Optionally it is also possible to display their attributes ("showColumns" parameter).  💡 To visualize the content of (a part of) a table, you can use "Table Visualization Data" script.

Arguments
---------

Optional inputs
~~~~~~~~~~~~~~~

``showColumns`` — *Display columns of the tables*
   Would you also like to display the column name in the tables?💡 Note : A small yellow key symbol (🔑) will appear if the column as a Primary Key constraint.

   Type: ``Boolean``

   Default: ``true``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

