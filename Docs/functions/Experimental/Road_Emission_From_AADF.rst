Road_Emission_From_AADF
=======================

Compute road emission noise levels from estimated annual average daily flows.

Overview
--------

``Road_Emission_From_AADF.groovy`` computes a road traffic emission table from estimated annual average daily flows, or AADF.

The script first converts average traffic to hourly traffic, then calculates day, evening, and night emission levels using the distribution described in the script comment.

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

* ``exec(connection, input)``
* ``computeLw(Long pk, Geometry geom, SpatialResultSet rs)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It checks that the source table contains a geometry column and an integer primary key.
* It recreates ``LW_ROADS`` with day, evening, and night octave-band columns.
* It computes hourly distributions from the ``AADF`` field and road category information.
* It updates the output geometry to ``Z = 0.05`` and adds an auto-increment primary key to ``LW_ROADS``.

