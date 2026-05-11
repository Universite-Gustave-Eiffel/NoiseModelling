Create_Assimilated_Maps
=======================

Create the assimilated result table.

Overview
--------

``Create_Assimilated_Maps.groovy`` creates a result table by joining the best configuration table with a receivers level table.

The output table contains timestamped receiver noise levels and geometries.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``bestConfigTable``
   Name of the best configuration table.

   Type: ``String``

``receiverLevel``
   Name of the receivers level table.

   Type: ``String``

``outputTable``
   Name of the output table to create.

   Type: ``String``

Output
------

``result``
   Name of the created result table.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, inputs)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It drops the output table if it already exists.
* It creates the output table from a join between the best configuration table and the receiver levels table using ``a.PERIOD = b.IT``.
* The created table contains ``TIMESTAMP``, ``LAEQ``, ``THE_GEOM``, and ``IDRECEIVER``.
* After creation, it converts the ``TIMESTAMP`` column to ``INTEGER``.

