DynamicIndicators
=================

Compute dynamic indicators.

Overview
--------

``DynamicIndicators.groovy`` computes dynamic noise indicators such as ``L10`` and ``L90`` from repeated sound level values stored in a table.

The input table is expected to contain multiple sound level values for each receiver geometry.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``columnName``
   Column name on which to perform the calculation.

   Example: ``LEQA``

   Type: ``String``

``tableName``
   Name of the table on which to perform the calculation.

   The table must contain multiple sound level values for a single receiver.

   Example: ``RECEIVERS_LEVEL``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``outputTableName``
   Name of the output table.

   Default: ``<tableName>_DYN_IND``

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* The input column and table names are normalized to uppercase.
* If no output table name is provided, the script writes to ``<TABLE>_DYN_IND``.
* The output table contains ``THE_GEOM``, ``L50``, ``L10``, and ``L90`` computed with SQL aggregate functions.

