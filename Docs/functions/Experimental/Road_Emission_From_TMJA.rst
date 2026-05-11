Road_Emission_From_TMJA
=======================

Compute road emission noise levels from estimated annual average daily flows using TMJA-based inputs.

Overview
--------

``Road_Emission_From_TMJA.groovy`` computes a road traffic emission table from estimated annual average daily flows labeled as TMJA.

The output table is ``LW_ROADS``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``sourcesTableName``
   Name of the source table.

   Default: ``SOURCES``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``databaseName``
   Name of the database.

   The script metadata documents a default of the first found database.

   Type: ``String``

Output
------

``result``
   Result string indicating that ``LW_ROADS`` has been created.

   Type: ``String``

Function Signatures
-------------------

The script exposes two functions:

* ``exec(Connection connection, Map input)``
* ``computeLw(Long pk, Geometry geom, SpatialResultSet rs)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It checks that the source table contains a geometry column and an integer primary key.
* It recreates ``LW_ROADS`` with day, evening, and night octave-band columns.
* It reads per-period traffic and speed inputs such as ``Q_VL_D``, ``Q_PL_D``, ``V_VL_D``, and ``V_PL_D`` from the source table.
* It updates the output geometry to ``Z = 0.05`` and adds an auto-increment primary key to ``LW_ROADS``.

