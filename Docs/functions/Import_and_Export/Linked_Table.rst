.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Linked Table
============

Overview
--------

➡️ Create a table into the database linked to an external database. The data is not stored into the database

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``localTableName``
   Name of the local linked table.

``databaseUrl``
   Connection url of the database. For PostGIS
   jdbc:postgresql_h2://hostname:5432/databaseName for H2
   jdbc:h2:tcp://localhost/D:/data/test

``username``
   User name when connecting to the external database

``password``
   User password when connecting to the external database

``remoteTableName``
   External Table name or query. If a query is used instead of the original table name, the table is read only. Queries must be enclosed in parenthesis: (SELECT * FROM ORDERS).

Optional inputs
~~~~~~~~~~~~~~~

``driverClass``
   Name of the class to connect to the external database.

``remoteSchemaName``
   External Table Schema ex: public

``force``
   Create the LINKED TABLE even if the remote database/table does not exist.

``fetchSize``
   the number of rows fetched, a hint with non-negative number of rows to fetch from the external table at once, may be ignored by the driver of external database. 0 is default and means no hint. The value is passed to java.sql.Statement.setFetchSize() method.

Output
------

``result``
   The name of the local linked table, can be used as an input for another process

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
