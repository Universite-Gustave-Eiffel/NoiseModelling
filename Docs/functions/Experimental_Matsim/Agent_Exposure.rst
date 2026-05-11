Agent_Exposure
==============

Calculate MATSim agents' noise exposure.

Overview
--------

``Agent_Exposure.groovy`` loads MATSim plans and experienced plans files and calculates agent noise exposure from previously computed time-sliced noise maps at receiver positions linked to MATSim activities.

It creates two output tables:

* the main exposure table named from ``outTableName``
* a per-time-bin sequence table named ``<outTableName>_SEQUENCE``

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``plansFile``
   Path to the MATSim ``output_plans`` file.

   Type: ``String``

``experiencedPlansFile``
   Path to the MATSim ``output_experienced_plans`` file.

   Type: ``String``

``receiversTable``
   Table containing receiver positions.

   The table must contain:

   * ``PK``
   * ``FACILITY``
   * ``ORIGIN_GEOM``
   * ``THE_GEOM``
   * ``TYPES``

   Type: ``String``

``dataTable``
   Table containing the noise data.

   The table must contain:

   * ``PK``
   * ``IDRECEIVER``
   * ``THE_GEOM``
   * ``HZ63`` to ``HZ8000``
   * ``PERIOD``

   Type: ``String``

``timeBinSize``
   Size of time bins in seconds.

   The stored time value is the start of each time bin.

   Type: ``Integer``

``outTableName``
   Name of the output table to create.

   The table contains fields such as ``PK``, ``PERSON_ID``, ``HOME_FACILITY``, ``HOME_GEOM``, ``WORK_FACILITY``, ``WORK_GEOM``, and ``LAEQ``.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``personsCsvFile``
   Path to the MATSim persons CSV file.

   Type: ``String``

``SRID``
   Projection identifier of the output geometry.

   Default: ``4326``

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

* It validates that the required MATSim files exist before processing.
* It checks and creates indexes on ``IDRECEIVER``, ``PERIOD``, and ``FACILITY`` when needed.
* It can enrich person attributes from an optional persons CSV file.
* It computes per-person overall ``LAEQ`` and also writes a per-time-bin activity sequence table.

