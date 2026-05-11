Linked_Table
============

Create a linked table to an external database.

Overview
--------

``Linked_Table.groovy`` creates a database table linked to an external database.

The data stays in the external database and is not copied locally.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``localTableName``
   Name of the local linked table to create.

   Type: ``String``

``databaseUrl``
   JDBC connection URL of the external database.

   Type: ``String``

``username``
   User name for the external database.

   Type: ``String``

``password``
   Password for the external database.

   Type: ``String``

``remoteTableName``
   External table name or query.

   If a query is used, it must be enclosed in parentheses and the linked table becomes read-only.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``driverClass``
   JDBC driver class used to connect to the external database.

   Allowed values include ``org.h2gis.postgis_jts.Driver`` and ``org.h2.Driver``.

   Default: ``org.h2gis.postgis_jts.Driver``

   Type: ``String``

``remoteSchemaName``
   External schema name.

   Default: ``public``

   Type: ``String``

``force``
   Whether to create the linked table even if the remote database or table does not exist.

   Type: ``Boolean``

``fetchSize``
   Fetch size hint passed to the linked-table statement.

   Type: ``Integer``

Output
------

``result``
   Name of the created local linked table.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input, ProgressVisitor progress)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It builds a ``CREATE LINKED TABLE`` statement dynamically from the provided connection settings.
* If ``fetchSize`` is provided, it appends a ``FETCH_SIZE`` clause.
* If ``force`` is enabled, it uses the ``FORCE`` keyword when creating the linked table.

