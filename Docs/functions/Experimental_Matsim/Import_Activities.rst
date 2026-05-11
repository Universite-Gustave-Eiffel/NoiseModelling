Import_Activities
=================

Import a MATSim facilities file.

Overview
--------

``Import_Activities.groovy`` imports a MATSim ``facilities`` file containing agent activity locations.

It creates a SQL table containing facility identifiers, geometries, and activity types.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``facilitiesPath``
   Path to the MATSim facilities file.

   Type: ``String``

``outTableName``
   Name of the table to create.

   The created table contains:

   * ``PK``
   * ``FACILITY``
   * ``THE_GEOM``
   * ``TYPES``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``SRID``
   Projection identifier of the table geometry.

   Default: ``4326``

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It drops the output table if it already exists.
* It creates indexes on ``FACILITY`` and on the geometry column.
* It reads MATSim facilities into a scenario and inserts one row per facility.
* It exports each facility as a ``POINTZ`` geometry using a fixed height of ``4.0`` meters.

