NMs_4_BestConfigs
=================

Create dynamic road traffic emission from the best configurations.

Overview
--------

``NMs_4_BestConfigs.groovy`` creates a dynamic road emission table using the best configurations.

The resulting table is ``LW_ROADS_best``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``bestConfig``
   Best configuration table, typically ``BEST_CONFIGURATION_FULL``.

   Type: ``String``

``roadEmission``
   Road emission table, typically ``LW_ROADS``.

   Type: ``String``

Output
------

``results``
   Dynamic road emission table using the best configuration: ``LW_ROADS_best``.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, inputs)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It drops ``LW_ROADS_best`` if it already exists.
* It creates ``LW_ROADS_best`` by selecting distinct road-emission rows whose ``PERIOD`` matches a best-configuration ``IT`` value.
* It adds an index on ``IDSOURCE`` and ``PERIOD`` for the resulting table.

