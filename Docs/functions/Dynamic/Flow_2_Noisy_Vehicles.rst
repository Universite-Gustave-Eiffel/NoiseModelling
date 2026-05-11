Flow_2_Noisy_Vehicles
=====================

Convert road traffic flows into noisy individual vehicles.

Overview
--------

``Flow_2_Noisy_Vehicles.groovy`` calculates individual vehicle positions and instantaneous noise levels from average traffic flows.

It creates two output tables:

* ``SOURCES_GEOM`` for source-point geometry, used to compute the attenuation matrix
* ``SOURCES_EMISSION`` for per-source, per-period octave-band emission levels

``SOURCES_GEOM`` contains:

* ``IDSOURCE``: identifier
* ``ROAD_ID``: identifier linked to the road segment
* ``THE_GEOM``: 3D source geometry as points

``SOURCES_EMISSION`` contains:

* ``PK``: identifier
* ``IDSOURCE``: link to the source point
* ``PERIOD``: timestamp iteration
* ``HZ63`` to ``HZ8000``: instantaneous emission sound level for each octave band

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads``
   Roads table name.

   The table must contain at least:

   * ``PK``: primary key identifier
   * ``LV_D``: hourly average light-vehicle count
   * ``HGV_D``: hourly average heavy-vehicle count
   * ``LV_SPD_D``: hourly average light-vehicle speed
   * ``HGV_SPD_D``: hourly average heavy-vehicle speed
   * ``PVMT``: CNOSSOS pavement identifier

   This table can be generated from the ``Import_OSM`` WPS block.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``method``
   Selected modeling method.

   Allowed values:

   * ``PROBA``: probabilistic representation of vehicle appearances for each time step
   * ``TNP``: simplified vehicle movements with temporal coherence

   Type: ``String``

``timestep``
   Time step in seconds.

   Default: ``1``

   Type: ``Integer``

``gridStep``
   Distance in meters between vehicle locations along the network.

   Default: ``10``

   Type: ``Integer``

``duration``
   Number of seconds to compute.

   Default: ``60``

   Type: ``Integer``

Output
------

``result``
   Name of the generated table that can be used by other processes.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It validates that the roads table uses a metric SRID.
* It densifies the road network into points, then creates ``SOURCES_GEOM`` with source heights forced to ``0.05`` meters.
* With ``PROBA``, it creates a probabilistic vehicle table and computes source emissions independently per time step.
* With ``TNP``, it simulates vehicle motion along each road segment and updates source levels over time.
* It creates an index on ``SOURCES_EMISSION(PERIOD, IDSOURCE)``.

