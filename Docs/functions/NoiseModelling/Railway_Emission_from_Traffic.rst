Railway_Emission_from_Traffic
=============================

Compute railway emission noise map from vehicle, traffic table, and section table.

Overview
--------

``Railway_Emission_from_Traffic.groovy`` computes railway emission noise levels from day, evening, and night traffic flow and speed estimates.

The output table is called ``LW_RAILWAY``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRailwayTraffic``
   Railway traffic table name.

   The table must contain:

   * ``IDTRAFFIC``: traffic identifier, primary key
   * ``IDSECTION``: section identifier linked to ``RAIL_SECTIONS``
   * ``TRAINTYPE``: vehicle type listed in the referenced CNOSSOS railway vehicle file
   * ``TRAINSPD``: maximum train speed in km/h
   * ``TDAY``, ``TEVENING``, ``TNIGHT``: hourly average train counts for day, evening, and night

   Type: ``String``

``tableRailwayTrack``
   Railway track geometry table name.

   The table must contain:

   * ``IDSECTION``: section identifier, primary key
   * ``NTRACK``: number of tracks
   * ``TRACKSPD``: maximum speed on the section in km/h
   * ``TRANSFER``: track transfer function identifier
   * ``ROUGHNESS``: rail roughness identifier
   * ``IMPACT``: impact noise coefficient identifier
   * ``CURVATURE``: curvature code
   * ``BRIDGE``: bridge transfer function identifier
   * ``ISTUNNEL``: whether the section is a tunnel

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``vehicleDataFile``
   URL of the railway vehicle data file in CNOSSOS JSON format.

   By default, the file bundled with NoiseModelling is used.

   Type: ``String``

``trainSetDataFile``
   URL of the railway train set data file in CNOSSOS JSON format.

   By default, the file bundled with NoiseModelling is used.

   Type: ``String``

``railwayEmissionDataFile``
   URL of the railway emission data file in CNOSSOS JSON format.

   By default, the file bundled with NoiseModelling is used.

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* The railway geometry table SRID is validated and must use a metric projection.
* The railway track table must contain a geometry column.
* The script writes the result into ``LW_RAILWAY`` using ``EmissionTableGenerator.makeTrainLWTable``.
* If custom JSON resource files are not supplied, the built-in CNOSSOS railway resources are used.
* After generation, the output geometry column SRID is explicitly set to match the source track SRID.

