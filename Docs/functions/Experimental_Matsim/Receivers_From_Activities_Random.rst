Receivers_From_Activities_Random
================================

Choose random receivers for MATSim activities based on nearby buildings.

Overview
--------

``Receivers_From_Activities_Random.groovy`` first finds a nearby building for each MATSim activity, then chooses a random receiver associated with that building.

The output table stores both the chosen receiver geometry and the original activity geometry.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``activitiesTable``
   Name of the table containing the activities.

   The table must contain:

   * ``PK``
   * ``FACILITY``
   * ``THE_GEOM``
   * ``TYPES``

   Type: ``String``

``buildingsTable``
   Name of the table containing the buildings.

   The table must contain:

   * ``PK``
   * ``THE_GEOM``

   Type: ``String``

``receiversTable``
   Name of the table containing the receivers.

   The table must contain:

   * ``PK``
   * ``THE_GEOM``
   * ``BUILD_PK``

   Type: ``String``

``outTableName``
   Name of the table to create.

   The table contains:

   * ``PK``
   * ``FACILITY``
   * ``ORIGIN_GEOM``
   * ``THE_GEOM``
   * ``TYPES``
   * ``BUILD_PK``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``randomSeed``
   Random seed used for receiver selection.

   Default: ``1234``

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It checks for a spatial index on the buildings geometry and a standard index on ``BUILD_PK`` in the receivers table.
* For each activity, it searches nearby candidate buildings and then chooses a random receiver attached to the first suitable building.
* If no receiver is found, the output geometry falls back to the activity geometry updated to ``Z = 4.0``.
* It uses a temporary table to store the chosen activity-building-receiver associations before building the final output.

