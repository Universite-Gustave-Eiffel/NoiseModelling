.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Linked Table
============

Overview
--------

➡️ Create a table into the database linked to an external database. The data is not stored into the database

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``databaseUrl`` — *Database URL*
   Connection url of the database. For PostGIS
   jdbc:postgresql_h2://hostname:5432/databaseName. For H2
   jdbc:h2:tcp://localhost/D:/data/test

   Type: ``String``

``localTableName`` — *Name of created table*
   Name of the local linked table.

   Type: ``String``

``lt_password`` — *User password*
   User password when connecting to the external database

   Type: ``String``

``lt_username`` — *User name*
   User name when connecting to the external database

   Type: ``String``

``remoteTableName`` — *External table name*
   External Table name or query. If a query is used instead of the original table name, then the table is read only. Queries must be enclosed in parenthesis: (SELECT * FROM ORDERS).

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``driverClass`` — *Driver name*
   Name of the class to connect to the external database.

   Type: ``String``

   Default: ``org.h2gis.postgis_jts.Driver``

   Allowed values: ``org.h2gis.postgis_jts.Driver``, ``org.h2.Driver``

``fetchSize`` — *Fetch size*
   the number of rows fetched, a hint with non-negative number of rows to fetch from the external table at once, may be ignored by the driver of external database. 0 is default and means no hint. The value is passed to java.sql.Statement.setFetchSize() method.

   Type: ``Integer``

   Default: ``0``

``force`` — *Force*
   Create the LINKED TABLE even if the remote database/table does not exist.

   Type: ``Boolean``

``remoteSchemaName`` — *External table schema*
   External Table Schema ex: public

   Type: ``String``

   Default: ``public``

Output
------

``result`` — *Local table name*
   The name of the local linked table, can be used as an input for another process

   Type: ``String``

