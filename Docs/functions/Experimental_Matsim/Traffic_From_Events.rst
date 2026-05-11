Traffic_From_Events
===================

Import traffic data from a MATSim simulation output folder.

Overview
--------

``Traffic_From_Events.groovy`` reads MATSim simulation outputs and generates NoiseModelling input tables for roads and time-sliced source emission levels.

It creates at least two tables:

* a geometry table named from ``outTableName``
* a source-emission table named ``<outTableName>_LW``

It can also create additional traffic and vehicle-contribution tables.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``folder``
   Path to the MATSim output folder.

   The folder must contain at least:

   * ``output_network.xml.gz``
   * ``output_allVehicles.xml.gz``
   * ``output_events.xml.gz``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``timeBinSize``
   Size of time bins in seconds.

   Default: ``3600``

   Type: ``Integer``

``timeBinMin``
   Minimum time-bin boundary in seconds.

   Default: ``0``

   Type: ``Integer``

``timeBinMax``
   Maximum time-bin boundary in seconds.

   Default: ``86400``

   Type: ``Integer``

``populationFactor``
   Population factor of the MATSim simulation.

   Must be between ``0`` and ``1``.

   Default: ``1.0``

   Type: ``Double``

``link2GeometryFile``
   Path to the pt2matsim CSV file containing link IDs and WKT geometries.

   If not set, the script falls back to geometries derived from the MATSim network.

   Type: ``String``

``SRID``
   Projection identifier of the geometry data.

   Default: ``4326``

   Type: ``Integer``

``exportTraffic``
   Whether to export average speed and flow per vehicle category in an additional table.

   Default: ``false``

   Type: ``Boolean``

``skipUnused``
   Whether to skip links with unused traffic in the output.

   Default: ``true`` according to the metadata description, while the inline code initializes the runtime default differently unless the parameter is provided.

   Type: ``Boolean``

``outTableName``
   Base name of the output table to create.

   The script also creates a table with a ``_LW`` suffix, and optionally ``_TRAFFIC`` and ``_CONTRIB`` tables.

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

* It validates the presence of the MATSim events, network, and vehicles files before processing.
* It reads the MATSim network, vehicles, and events, then aggregates trips per link and per time bin.
* It creates a geometry table, a ``_LW`` emission table, and optional ``_TRAFFIC`` and ``_CONTRIB`` tables.
* It computes octave-band source levels from per-link vehicle flows and average travel times.
* It creates spatial and non-spatial indexes on the generated tables after insertion.

