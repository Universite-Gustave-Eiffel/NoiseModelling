Ind_Vehicles_2_Noisy_Vehicles
=============================

Convert individual vehicles into emission noise levels and snap them to network point sources.

Overview
--------

``Ind_Vehicles_2_Noisy_Vehicles.groovy`` calculates dynamic road emissions from individual vehicle trajectories and snaps them to the network source points.

The output table is ``SOURCES_EMISSION`` and contains:

* ``IDSOURCE``: identifier
* ``PERIOD``: timestamp iteration
* ``HZ63`` to ``HZ8000``: emission sound level for each octave band

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableVehicles``
   Individual vehicles table.

   It should contain timestep, geometry as ``POINT``, speed, acceleration, vehicle type, and similar trajectory attributes.

   Type: ``String``

``tableSourceGeom``
   Source geometry table.

   This is the point-source geometry table to which the computed emissions are reattached according to the snap distance. It is expected to be similar to ``SOURCES_GEOM``.

   Type: ``String``

``distance2snap``
   Maximum distance used to snap vehicles to network point sources.

   Type: ``Double``

``tableFormat``
   Format of the individual vehicles table.

   The script currently documents ``SUMO`` and ``Matsim``-style formats, while its inline code also handles ``SYMUVIA``.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``keepNoEmissionGeoms``
   Whether source geometries without any emission value should be kept.

   Default behavior keeps them, which reduces computation time when later evaluating attenuation.

   Type: ``Boolean``

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

* It validates that the vehicles table uses a metric SRID.
* It requires the source-geometry table to have an integer primary key.
* It computes vehicle emission levels into an intermediate ``LW_VEHICLE`` table, then snaps each record to the nearest source geometry within the requested distance.
* If several vehicles are associated with the same source and period, their levels are merged energetically into ``SOURCES_EMISSION``.
* Depending on ``keepNoEmissionGeoms``, source geometries without associated emissions may be deleted from the source-geometry table.

