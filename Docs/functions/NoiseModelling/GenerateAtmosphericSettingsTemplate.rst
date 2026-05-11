GenerateAtmosphericSettingsTemplate
==================================

Generate default atmospheric settings from the ``PERIOD`` field of a noise emission table.

Overview
--------

``GenerateAtmosphericSettingsTemplate.groovy`` generates default atmospheric settings from the ``PERIOD`` field of a noise emission table.

It is intended to produce a table that can be exported, edited, reimported, and then used by ``Noise_level_from_source`` or ``Noise_level_from_traffic``.

This table lets you adjust temperature and other atmospheric settings for each simulation time period.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableSourcesEmission``
   Sources emission table name, for example ``SOURCES_EMISSION``.

   The table must contain:

   * ``IDSOURCE``: identifier linked to the primary key of the source table
   * ``PERIOD``: time period that will be reused in the generated atmospheric table

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``tablePeriodAtmosphericSettings``
   Name of the output atmospheric settings table for each time period.

   The generated table contains:

   * ``PERIOD``: time period (``VARCHAR PRIMARY KEY``)
   * ``WINDROSE``: probability of occurrences of favorable propagation conditions (``ARRAY(16)``)
   * ``TEMPERATURE``: temperature in Celsius
   * ``PRESSURE``: air pressure in pascal
   * ``HUMIDITY``: air humidity in percent
   * ``GDISC``: whether ground discontinuity is accepted. Default ``true``.
   * ``PRIME2520``: whether prime values are used to compute equation 2.5.20. Default ``false``.

   Default: ``SOURCES_ATMOSPHERIC``

   Type: ``String``

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

* It reads the unique ``PERIOD`` values from the source emission table.
* It creates one atmospheric settings row per period using ``AttenuationParameters`` defaults.
* If no output table name is provided, it writes to ``SOURCES_ATMOSPHERIC``.

