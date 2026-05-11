Noise_level_from_source
=======================

Computes the propagation from the sounds sources to the receivers.

Overview
--------

``Noise_level_from_source.groovy`` computes the propagation from the sound sources to the receiver locations using the noise emission table.

Tables must be projected in a metric coordinate system (SRID). Use ``Change_SRID`` if needed.

The output table is called ``RECEIVERS_LEVEL``.

The output table contains:

* ``IDRECEIVER``: receiver identifier linked to the ``RECEIVERS`` table primary key.
* ``IDSOURCE``: source identifier linked to the ``SOURCES_GEOM`` primary key. Present only if ``Keep source id`` is enabled.
* ``PERIOD``: time period such as ``L``, ``D``, ``E`` and ``DEN``. Present only if source emission power or atmospheric settings per period are provided.
* ``THE_GEOM``: 3D geometry of the receivers, with ``Z`` as altitude (``POINTZ``).
* ``Hz63``, ``Hz125``, ``Hz250``, ``Hz500``, ``Hz1000``, ``Hz2000``, ``Hz4000``, ``Hz8000``: sound level for each octave band.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableBuilding``
   Buildings table name.

   The table must contain:

   * ``THE_GEOM``: 2D geometry of the building (``POLYGON`` or ``MULTIPOLYGON``)
   * ``HEIGHT``: building height (``FLOAT``)
   * ``G``: optional wall absorption value if ``G`` is in ``[0, 1]``, or wall surface impedance if ``G`` is in ``[20, 20000]``. Default is ``0.1`` if the column does not exist.

   Type: ``String``

``tableSources``
   Sources geometry table name.

   The table must contain:

   * ``PK``: identifier. It shall be a primary key (``INTEGER, PRIMARY KEY``)
   * ``THE_GEOM``: 3D source geometry (``POINT``, ``MULTIPOINT``, ``LINESTRING``, ``MULTILINESTRING``). According to CNOSSOS-EU, road traffic emission should use a height of ``0.05 m``.
   * ``HZD63``, ``HZD125``, ``HZD250``, ``HZD500``, ``HZD1000``, ``HZD2000``, ``HZD4000``, ``HZD8000``: day emission sound level for each octave band
   * ``HZE``: evening emission sound level columns for each octave band
   * ``HZN``: night emission sound level columns for each octave band
   * ``YAW``: horizontal orientation in degrees. For points, ``0`` is north and ``90`` is east. For lines, ``0`` is line direction and ``90`` is right of line direction.
   * ``PITCH``: vertical orientation in degrees. ``0`` front, ``90`` top, ``-90`` bottom.
   * ``ROLL``: roll in degrees
   * ``DIR_ID``: directivity sphere identifier from ``tableSourceDirectivity`` or train directivity if not provided. Values include ``OMNIDIRECTIONAL(0)``, ``ROLLING(1)``, ``TRACTIONA(2)``, ``TRACTIONB(3)``, ``AERODYNAMICA(4)``, ``AERODYNAMICB(5)``, ``BRIDGE(6)``.

   This table can be generated from the ``Road_Emission_from_Traffic`` WPS block.

   Type: ``String``

``tableReceivers``
   Receivers table name.

   The table must contain:

   * ``PK``: identifier. It shall be a primary key (``INTEGER, PRIMARY KEY``)
   * ``THE_GEOM``: 3D geometry of the receivers (``POINT``, ``MULTIPOINT``)

   This table can be generated from WPS blocks in the ``Receivers`` folder.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``tableSourcesEmission``
   Sources emission table name, for example ``SOURCES_EMISSION``.

   The table must contain:

   * ``IDSOURCE``: identifier linked to the primary key of the source geometry table
   * ``PERIOD``: time period copied to the output
   * ``HZ63``, ``HZ125``, ``HZ250``, ``HZ500``, ``HZ1000``, ``HZ2000``, ``HZ4000``, ``HZ8000``: emission noise level in dB. Third-octave bands from ``50 Hz`` to ``10000 Hz`` can also be used.

   Type: ``String``

``tableDEM``
   Digital Elevation Model table name.

   The table must contain:

   * ``THE_GEOM``: 3D geometry (``POINT``, ``MULTIPOINT``)

   This table can be generated from ``Import_Asc_File``.

   Type: ``String``

``tableGroundAbs``
   Ground absorption table name.

   The table must contain:

   * ``THE_GEOM``: 2D geometry (``POLYGON`` or ``MULTIPOLYGON``)
   * ``G``: ground acoustic absorption between ``0`` (very hard) and ``1`` (very soft)

   Type: ``String``

``tableSourceDirectivity``
   Source directivity table name.

   If not specified, the default is CNOSSOS-EU train directivity.

   The table must contain:

   * ``DIR_ID``: directivity sphere identifier
   * ``THETA``: vertical angle in degrees, from ``-90`` to ``90``. ``0`` front, ``90`` top, ``-90`` bottom.
   * ``PHI``: horizontal angle in degrees, from ``0`` to ``360``. ``0`` front, ``90`` right.
   * ``HZ63``, ``HZ125``, ``HZ250``, ``HZ500``, ``HZ1000``, ``HZ2000``, ``HZ4000``, ``HZ8000``: attenuation in dB for each octave or third-octave band.

   Type: ``String``

``tablePeriodAtmosphericSettings``
   Atmospheric settings table name for each time period.

   The table must contain:

   * ``PERIOD``: time period (``VARCHAR PRIMARY KEY``)
   * ``WINDROSE``: probability of favorable propagation conditions (``ARRAY(16)``)
   * ``TEMPERATURE``: temperature in Celsius
   * ``PRESSURE``: air pressure in pascal
   * ``HUMIDITY``: air humidity in percent
   * ``GDISC``: whether ground discontinuity is accepted. Default ``true``.
   * ``PRIME2520``: whether prime values are used for equation 2.5.20. Default ``false``.

   Type: ``String``

``paramWallAlpha``
   Wall absorption coefficient in ``[0,1]``, where ``0`` is fully reflective and ``1`` is fully absorbent.

   Default: ``0.1``

   Type: ``Double``

``confReflOrder``
   Maximum number of reflections to take into account.

   Adding one more order of reflection can significantly increase processing time.

   Default: ``1``

   Type: ``Integer``

``confMaxSrcDist``
   Maximum source-receiver distance in meters.

   Default: ``150``

   Type: ``Double``

   .. figure:: ../../wps_images/acoustics_parameters_confMaxSrcDist.png
      :align: center
      :alt: Maximum source-receiver distance

``confMaxReflDist``
   Maximum search distance of walls or facades from the source-receiver segment for specular reflections, in meters.

   Default: ``50``

   Type: ``Double``

   .. figure:: ../../wps_images/acoustics_parameters_confMaxReflDist.png
      :align: center
      :alt: Maximum source-reflection distance

``confThreadNumber``
   Number of threads to use.

   Default: ``0`` meaning automatic detection of CPU cores minus one. For example, on an 8-core machine, 7 cores are used.

   Type: ``Integer``

``confDiffVertical``
   Whether diffraction on vertical edges is computed.

   Following Directive 2015/996, this should be enabled for rail and industrial sources only.

   Default: ``false``

   Type: ``Boolean``

``confDiffHorizontal``
   Whether diffraction on horizontal edges is computed.

   Default: ``false``

   Type: ``Boolean``

``confExportSourceId``
   Whether source identifiers are preserved in the output so the contribution of each source can be separated.

   Default: ``false``

   Type: ``Boolean``

``confHumidity``
   Relative humidity for noise propagation, in percent from ``0`` to ``100``.

   Default: ``70``

   Type: ``Double``

``confTemperature``
   Air temperature in degrees Celsius.

   Default: ``15``

   Type: ``Double``

``confFavourableOccurrencesDefault``
   Comma-delimited string containing the probability, between ``0`` and ``1``, of favorable propagation conditions.

   Values follow the clockwise direction. The north slice is the last array index, number 16 in the schema, not the first one.

   Default: ``0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5``

   Type: ``String``

   .. figure:: ../../wps_images/acoustics_parameters_confFavorableOccurrences.png
      :align: center
      :alt: Favorable propagation occurrence schema

``confRaysName``
   Export scene to a table, for example ``RAYS``, or to a file URL such as ``file:///Z:/dir/map.kml``.

   This can be used to save the terrain model, buildings, and propagation rays computed by NoiseModelling.

   The number of rays is limited in this script to avoid memory exceptions.

   Default: empty value, meaning rays are not kept.

   Type: ``String``

``confMaxError``
   Threshold in dB used to exclude negligible sound sources from calculations.

   Default: ``0.1``

   This parameter is ignored if no emission level is specified or if it is set to ``0 dB``. It has a strong impact on computation time.

   Type: ``Double``

``frequencyFieldPrepend``
   Prefix used for frequency field names.

   For example, for ``1000 Hz`` the default column name is ``HZ1000``.

   Default: ``HZ``

   Type: ``String``

Output
------

``result``
   Name of the table containing the computation results. It can be used as input for another process.

   Type: ``String``

Function Signatures
-------------------

The script exposes two entry points:

* ``exec(Connection connection, Map input, ProgressVisitor progress)``
* ``exec(Connection connection, Map input)``

The second form calls the first one with an ``EmptyProgressVisitor``.

Execution Notes
---------------

The script comments and inline checks show the following behavior:

* Source, receiver, building, DEM, and ground tables are validated for metric SRID compatibility.
* Source and receiver tables must have geometry columns and integer primary keys.
* The script drops ``RECEIVERS_LEVEL`` if it already exists before running.
* If ``tableSourceDirectivity`` is not provided, CNOSSOS-EU train directivity is used by default.
* If ``tableSourcesEmission`` is provided, period-based emissions are propagated into the output.
* If ``tablePeriodAtmosphericSettings`` is provided, atmospheric settings can vary by period.
