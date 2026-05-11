Noise_Map_Difference
====================

Compute the difference between two noise maps.

Overview
--------

``Noise_Map_Difference.groovy`` computes the difference between two noise-map tables and writes the result into a new output table.

The subtraction can be done in the default order or inverted.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``mainMapTable``
   Name of the table containing the primary noise-map data.

   The table must contain:

   * ``PK``
   * ``THE_GEOM``
   * ``HZ63`` to ``HZ8000``
   * ``LAEQ``
   * ``LEQ``

   Type: ``String``

``secondMapTable``
   Name of the table containing the secondary noise-map data.

   The table must contain:

   * ``PK``
   * ``THE_GEOM``
   * ``HZ63`` to ``HZ8000``
   * ``LAEQ``
   * ``LEQ``

   Type: ``String``

``outTable``
   Name of the output table to create.

   The created table contains:

   * ``PK``
   * ``THE_GEOM``
   * ``HZ63`` to ``HZ8000``
   * ``LAEQ``
   * ``LEQ``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``invert``
   Whether the subtraction should be inverted.

   * ``false``: primary map minus second map
   * ``true``: second map minus primary map

   Type: ``Boolean``

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

* It drops the requested output table if it already exists.
* It joins the two input maps on ``IDRECEIVER`` and ``TIMESTRING``.
* It computes per-band, ``LAEQ``, and ``LEQ`` differences directly in SQL.
* The optional ``invert`` flag changes the sign of the subtraction in the generated SQL query.

