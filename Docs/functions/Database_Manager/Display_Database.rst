.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

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

``showColumns``
   Do you want to display also the column of the tables ? 💡 Note : A small yellow key symbol (🔑) will appear if the column as a Primary Key constraint.

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
