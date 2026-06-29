.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Clean Database
==============

Delete all database tables

Overview
--------

➡️ Delete all non-system tables of the database. 🚨 Use with caution

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``areYouSure`` — *Are you sure?*
   Are you sure you want to delete all the tables in the database?

   Type: ``Boolean``

Optional inputs
~~~~~~~~~~~~~~~

``schema`` — *Schema*
   Database schema to clean. A schema is a container that organizes tables, views, and other database objects. If you are unsure, leave this set to the default schema, "public"

   Type: ``String``

   Default: ``public``

Output
------

``result`` — *Result output string*
   This type of result does not allow the blocks to be linked together.

   Type: ``String``

