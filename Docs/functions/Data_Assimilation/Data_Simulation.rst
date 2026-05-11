Data_Simulation
===============

Run the data-simulation process to generate noise maps.

Overview
--------

``Data_Simulation.groovy`` executes a series of operations to generate simulated noise maps from the combinations stored in ``ALL_CONFIGURATIONS``.

It creates the tables ``LW_ROADS`` and ``ROADS_GEOM``.

Arguments
---------

Optional inputs
~~~~~~~~~~~~~~~

``noiseMapLimit``
   Percentage, from ``1`` to ``100``, of the maximum number of map combinations to simulate.

   Default: ``100``

   Type: ``Integer``

Output
------

``result``
   Output tables ``LW_ROADS`` and ``ROADS_GEOM``.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It reads all parameter combinations from ``ALL_CONFIGURATIONS``.
* It creates ``ROADS_GEOM`` from the ``ROADS`` table and recreates ``LW_ROADS`` from scratch.
* It uses a sampling step based on the requested limit percentage and builds ``FILTERED_CONFIGURATIONS`` from the selected indices.
* For each selected configuration, it recalculates day-period road emission levels and inserts them into ``LW_ROADS``.
* It stores the selected configuration identifier in the ``PERIOD`` column.

