Split_Sources_Period
====================

Split a dynamic source table into geometry and emission tables.

Overview
--------

``Split_Sources_Period.groovy`` splits a single table that contains duplicated geometries and repeated source identifiers across multiple periods into separate ``SOURCES_GEOM`` and ``SOURCES_EMISSION`` tables.

The resulting tables can be used directly with ``Noise_level_from_source`` or ``Noise_From_Attenuation_Matrix``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableSourceDynamic``
   Source table name.

   The source table contains multiple periods for the same source index, along with other columns compatible with ``Noise_level_from_source`` or ``Noise_level_from_traffic`` style emission data.

   Type: ``String``

``sourceIndexFieldName``
   Field name for the source index.

   It is translated into ``IDSOURCE`` in the output tables.

   Type: ``String``

``sourcePeriodFieldName``
   Field name for the source period, for example ``T``.

   It is translated into ``PERIOD`` in the output tables.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``sourceGeomTableName``
   Output table name that contains the distinct source index and associated geometry.

   Default: ``SOURCES_GEOM``

   Type: ``String``

``sourceEmissionTableName``
   Output table name that contains, for each source index, the period and other source attributes.

   Default: ``SOURCES_EMISSION``

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It validates that the source table uses a metric SRID.
* It removes the geometry, period, and source-index columns from the list of additional attributes copied into the emission table.
* It creates the geometry table from distinct source identifiers using ``ANY_VALUE(THE_GEOM)``.
* It creates the emission table by renaming the chosen source-index and period fields to ``IDSOURCE`` and ``PERIOD``.
* It adds an index on ``IDSOURCE`` and ``PERIOD`` and updates the geometry SRID of the source-geometry table.

