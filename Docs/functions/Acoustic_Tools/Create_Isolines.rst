Create_Isolines
===============

Create isolines, also called isophones.

Overview
--------

``Create_Isolines.groovy`` generates isolines by linear interpolation on triangle edges using a marching-triangles approach.

It creates one multilines map per ``PERIOD`` and per ``LEVEL``.

The main output table is ``ISOLINES_NOISE_MAP`` and contains:

* ``PERIOD``: receiver period label
* ``LEVEL``: isoline value
* ``THE_GEOM``: ``MULTILINESTRING`` or ``LINESTRING`` geometry

Additional output tables are also created, one per period, named ``L<PERIOD>_ISOLINES_NOISE_MAP``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``trianglesTable``
   Name of the triangles table.

   It should contain: ``PK``, ``THE_GEOM``, ``PK_1``, ``PK_2``, ``PK_3``, ``CELL_ID``.

   Type: ``String``

``receiversTable``
   Name of the receivers level table.

   It should contain: ``IDRECEIVER``, ``PERIOD``, ``THE_GEOM``, and ``LAEQ`` or another numeric field to contour.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``fieldName``
   Numeric field from the receivers table to contour.

   Default: ``LAEQ``

   Type: ``String``

``isoClasses``
   Comma-separated contour levels in dB.

   Default: ``35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0``

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

* It validates that both the triangles table and the receivers table exist.
* It resolves the SRID from the receivers table first, then from the triangles table if needed.
* It creates an intermediate segments table and stitches segments into final isolines using ``ST_Union`` and ``ST_LineMerge``.
* It creates one additional output table per non-null ``PERIOD`` value.

