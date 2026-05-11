Road_Emission_from_Traffic
==========================

Compute road emission noise map from road table.

Overview
--------

``Road_Emission_from_Traffic.groovy`` computes road emission noise levels from traffic flow and speed estimates.

The output table is called ``LW_ROADS``.

If the input table contains a ``PERIOD`` field, period-based emissions are generated directly. Otherwise, the script expects day, evening, and night traffic columns and generates corresponding ``HZD*``, ``HZE*``, and ``HZN*`` fields.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads``
   Roads table name.

   The script recognizes the following documented fields:

   * ``PK``: if a primary key exists, it is copied and reused as the output primary key
   * ``IDSOURCE``: copied as-is when present, and expected if ``LW_ROADS`` will later be used as ``SOURCES_EMISSION`` for ``Noise_level_from_source``
   * ``PERIOD``: period label such as ``D``, ``E``, ``N``, or ``DEN`` when period-based input is used
   * ``LV``, ``MV``, ``HGV``, ``WAV``, ``WBV``: hourly average traffic counts
   * ``LV_SPD``, ``MV_SPD``, ``HGV_SPD``, ``WAV_SPD``, ``WBV_SPD``: hourly average speeds
   * ``LV_D``, ``LV_E``, ``LV_N``: day, evening, and night light vehicle counts
   * ``MV_D``, ``MV_E``, ``MV_N``: day, evening, and night medium heavy vehicle counts
   * ``HGV_D``, ``HGV_E``, ``HGV_N``: day, evening, and night heavy duty vehicle counts
   * ``WAV_D``, ``WAV_E``, ``WAV_N``: day, evening, and night light powered two-wheeler counts
   * ``WBV_D``, ``WBV_E``, ``WBV_N``: day, evening, and night heavier powered two-wheeler counts
   * ``LV_SPD_D``, ``LV_SPD_E``, ``LV_SPD_N``: day, evening, and night light vehicle speeds
   * ``MV_SPD_D``, ``MV_SPD_E``, ``MV_SPD_N``: day, evening, and night medium heavy vehicle speeds
   * ``HGV_SPD_D``, ``HGV_SPD_E``, ``HGV_SPD_N``: day, evening, and night heavy duty vehicle speeds
   * ``WAV_SPD_D``, ``WAV_SPD_E``, ``WAV_SPD_N``: day, evening, and night light powered two-wheeler speeds
   * ``WBV_SPD_D``, ``WBV_SPD_E``, ``WBV_SPD_N``: day, evening, and night heavier powered two-wheeler speeds
   * ``PVMT``: CNOSSOS road pavement identifier, for example ``NL05``. Default ``NL08``.
   * ``TS_STUD``: studded-tyre duration over the year in months
   * ``PM_STUD``: average proportion of vehicles equipped with studded tyres
   * ``JUNC_DIST``: distance to junction in meters
   * ``JUNC_TYPE``: junction type
   * ``SLOPE``: road slope in percent
   * ``WAY``: traffic direction mode

   This table can be generated from the ``Import_OSM`` WPS block.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``coefficientVersion``
   CNOSSOS coefficient version.

   ``1`` means 2015 and ``2`` means 2020.

   Default: ``2``

   Type: ``Double``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* The output table ``LW_ROADS`` is dropped and recreated each time the script runs.
* If the input includes a geometry column without Z coordinates, the script forces the output geometry to ``Z = 0.05`` meters.
* If a primary key exists on the input roads table, it is copied to the output table and restored as the output primary key.
* If the input contains a ``PERIOD`` field, the output uses ``HZ63`` to ``HZ8000`` columns for that period.
* Otherwise, the output uses day, evening, and night frequency columns such as ``HZD63``, ``HZE63``, and ``HZN63``.

