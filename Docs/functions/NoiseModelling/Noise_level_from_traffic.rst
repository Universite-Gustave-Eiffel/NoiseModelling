Noise_level_from_traffic
========================

Compute noise level directly from road traffic data.

Overview
--------

``Noise_level_from_traffic.groovy`` computes a noise map for each period from traffic flow rate and speed estimates.

Tables must be projected in a metric coordinate system (SRID). Use ``Change_SRID`` if needed.

The output table is ``RECEIVERS_LEVEL``.

The output table contains:

* ``IDRECEIVER``: receiver identifier
* ``IDSOURCE``: source identifier if source IDs are preserved
* ``THE_GEOM``: 3D geometry of the receivers
* ``PERIOD``: time period such as ``D``, ``E``, ``N``, or ``DEN``
* ``Lw63``, ``Lw125``, ``Lw250``, ``Lw500``, ``Lw1000``, ``Lw2000``, ``Lw4000``, ``Lw8000``, ``Laeq``, ``Leq``: receiver noise levels

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableBuilding``
   Buildings table name.

   The table must contain:

   * ``THE_GEOM``: 2D building geometry (``POLYGON`` or ``MULTIPOLYGON``)
   * ``HEIGHT``: building height

   Type: ``String``

``tableRoads``
   Roads table name.

   Traffic can be provided directly in this table, but in that form it is limited to day, evening, and night periods.

   Recognized columns include:

   * ``PK``: primary key identifier
   * ``LV_D``, ``LV_E``, ``LV_N``: hourly average light vehicle count
   * ``MV_D``, ``MV_E``, ``MV_N``: hourly average medium heavy vehicle count
   * ``HGV_D``, ``HGV_E``, ``HGV_N``: hourly average heavy duty vehicle count
   * ``WAV_D``, ``WAV_E``, ``WAV_N``: hourly average moped, tricycle, or quad count up to 50 cc
   * ``WBV_D``, ``WBV_E``, ``WBV_N``: hourly average motorcycle, tricycle, or quad count above 50 cc
   * ``LV_SPD_D``, ``LV_SPD_E``, ``LV_SPD_N``: light vehicle speed
   * ``MV_SPD_D``, ``MV_SPD_E``, ``MV_SPD_N``: medium heavy vehicle speed
   * ``HGV_SPD_D``, ``HGV_SPD_E``, ``HGV_SPD_N``: heavy duty vehicle speed
   * ``WAV_SPD_D``, ``WAV_SPD_E``, ``WAV_SPD_N``: light powered two-wheeler speed
   * ``WBV_SPD_D``, ``WBV_SPD_E``, ``WBV_SPD_N``: heavier powered two-wheeler speed
   * ``PVMT``: CNOSSOS road pavement identifier, for example ``NL05``. Default ``NL08``.
   * ``TEMP_D``, ``TEMP_E``, ``TEMP_N``: average temperature for day, evening, and night. Default ``20 C``.
   * ``TS_STUD``: studded-tyre duration over the year in months
   * ``PM_STUD``: average proportion of vehicles using studded tyres
   * ``JUNC_DIST``: distance to junction in meters
   * ``JUNC_TYPE``: junction type
   * ``SLOPE``: road section slope in percent
   * ``WAY``: traffic direction mode

   This table can be generated from the ``Import_OSM`` WPS block.

   Type: ``String``

``tableReceivers``
   Receivers table name.

   The table must contain:

   * ``PK``: primary key identifier
   * ``THE_GEOM``: receiver geometry (``POINT`` or ``MULTIPOINT``)

   This table can be generated from WPS blocks in the ``Receivers`` folder.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``tableRoadsTraffic``
   Road traffic table per period.

   Recognized columns include:

   * ``IDSOURCE``: identifier linked to the primary key of ``tableRoads``
   * ``PERIOD``: time period
   * ``LV``, ``MV``, ``HGV``, ``WAV``, ``WBV``: hourly average traffic counts
   * ``LV_SPD``, ``MV_SPD``, ``HGV_SPD``, ``WAV_SPD``, ``WBV_SPD``: hourly average speeds
   * ``PVMT``: CNOSSOS road pavement identifier
   * ``TS_STUD``: studded-tyre duration
   * ``PM_STUD``: studded-tyre proportion
   * ``JUNC_DIST``: distance to junction in meters
   * ``JUNC_TYPE``: junction type
   * ``SLOPE``: road slope in percent
   * ``WAY``: traffic direction mode

   Type: ``String``

``tableSourceDirectivity``
   Source directivity table name.

   If not specified, the default is CNOSSOS-EU train directivity.

   The table must contain:

   * ``DIR_ID``: directivity sphere identifier
   * ``THETA``: vertical angle from ``-90`` to ``90`` degrees
   * ``PHI``: horizontal angle from ``0`` to ``360`` degrees
   * ``LW63``, ``LW125``, ``LW250``, ``LW500``, ``LW1000``, ``LW2000``, ``LW4000``, ``LW8000``: attenuation in dB for each band

   Type: ``String``

``tablePeriodAtmosphericSettings``
   Atmospheric settings table name for each time period.

   The table must contain:

   * ``PERIOD``: time period
   * ``WINDROSE``: probability of favorable propagation conditions
   * ``TEMPERATURE``: temperature in Celsius
   * ``PRESSURE``: air pressure in pascal
   * ``HUMIDITY``: air humidity in percent
   * ``GDISC``: whether ground discontinuity is accepted
   * ``PRIME2520``: whether prime values are used for equation 2.5.20

   Type: ``String``

``tableDEM``
   DEM table name.

   The table must contain:

   * ``THE_GEOM``: 3D geometry

   This table can be generated from ``Import_Asc_File``.

   Type: ``String``

``tableGroundAbs``
   Ground acoustic absorption table name.

   The table must contain:

   * ``THE_GEOM``: polygon geometry
   * ``G``: ground absorption between ``0`` and ``1``

   Type: ``String``

``paramWallAlpha``
   Wall absorption coefficient between ``0`` and ``1``.

   Default: ``0.1``

   Type: ``Double``

``confReflOrder``
   Maximum reflection order.

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
   Maximum search distance of walls or facades from the source-receiver segment, in meters, for specular reflections.

   Default: ``50``

   Type: ``Double``

   .. figure:: ../../wps_images/acoustics_parameters_confMaxReflDist.png
      :align: center
      :alt: Maximum source-reflection distance

``confThreadNumber``
   Number of threads to use.

   Default: ``0`` meaning automatic core detection minus one.

   Type: ``Integer``

``confDiffVertical``
   Whether diffraction on vertical edges is computed.

   Default: ``false``

   Type: ``Boolean``

``confDiffHorizontal``
   Whether diffraction on horizontal edges is computed.

   Default: ``false``

   Type: ``Boolean``

``confExportSourceId``
   Whether source identifiers are preserved in the output.

   Default: ``false``

   Type: ``Boolean``

``confHumidity``
   Relative humidity for propagation, in percent.

   Default: ``70``

   Type: ``Double``

``confTemperature``
   Air temperature in degrees Celsius.

   Default: ``15``

   Type: ``Double``

``confFavourableOccurrencesDefault``
   Comma-delimited probabilities, between ``0`` and ``1``, for favorable propagation conditions.

   The values follow the clockwise direction, with north stored as array index 16.

   Default: sixteen values of ``0.5``

   Type: ``String``

   .. figure:: ../../wps_images/acoustics_parameters_confFavorableOccurrences.png
      :align: center
      :alt: Favorable propagation occurrence schema

``confRaysName``
   Export scene to a table such as ``RAYS`` or to a file URL such as ``file:///Z:/dir/map.kml``.

   Default: empty value, meaning rays are not kept.

   Type: ``String``

``confMaxError``
   Threshold in dB for excluding negligible sound sources.

   Default: ``0.1``

   Type: ``Double``

``frequencyFieldPrepend``
   Prefix for frequency field names.

   Default: ``HZ``

   Type: ``String``

``coefficientVersion``
   CNOSSOS coefficient version.

   ``1`` means 2015 and ``2`` means 2020.

   Default: ``2``

   Type: ``Integer``

Output
------

``result``
   Name of the table containing computation results. It can be reused as input for another process.

   Type: ``String``

Function Signatures
-------------------

The script exposes two entry points:

* ``exec(Connection connection, Map input, ProgressVisitor progress)``
* ``exec(Connection connection, Map input)``

The second form calls the first one with an ``EmptyProgressVisitor``.

Execution Notes
---------------

The script comments and inline behavior show the following:

* Source, receiver, building, DEM, and ground tables are validated for metric SRID compatibility.
* Source and receiver tables must have geometry columns and integer primary keys.
* If ``tableRoadsTraffic`` is provided, emissions are read from the period-based traffic table.
* If ``tableSourceDirectivity`` is not provided, CNOSSOS-EU train directivity is used by default.
* If ``tablePeriodAtmosphericSettings`` is provided, atmospheric settings can vary by period.
* If ``confRaysName`` is set, propagation rays can be exported, with the script limiting the exported ray count.

