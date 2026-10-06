.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Execute Query
=============

Run SQL queries and display the results

Overview
--------

➡️ Run multiple SQL queries and display the results.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``sqlQueries`` — *SQL queries*
   SQL queries (e.g., CREATE TABLE mytable AS SELECT * FROM othertable; SELECT * FROM mytable;)

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``outputFormat`` — *Output format*
   Choose the output format for the result.

   Type: ``String``

   Default: ``HTML``

   Allowed values: ``HTML``, ``JSON``

Output
------

``result`` — *Result output string*
   The output of the SQL query execution, formatted according to the selected output format.

   Type: ``String``

