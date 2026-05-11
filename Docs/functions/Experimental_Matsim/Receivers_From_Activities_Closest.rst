Receivers_From_Activities_Closest
=================================

Choose the closest receivers for MATSim activities.

Overview
--------

``Receivers_From_Activities_Closest.groovy`` assigns the closest receiver from a receivers table to every MATSim activity in an activities table.

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

``receiversTable``
   Name of the table containing the receivers.

   The table must contain:

   * ``PK``
   * ``THE_GEOM``

   Type: ``String``

``outTableName``
   Name of the table to create.

   The table contains:

   * ``PK``
   * ``FACILITY``
   * ``ORIGIN_GEOM``
   * ``THE_GEOM``
   * ``TYPES``

   Type: ``String``

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

* It checks for spatial indexes on the activity and receiver geometry columns and creates them if missing.
* For each activity, it searches the closest receiver within an expanded bounding box.
* If no receiver is found nearby, it falls back to the activity geometry itself.
* It updates both receiver and origin geometries to a Z value of ``4.0`` meters.

