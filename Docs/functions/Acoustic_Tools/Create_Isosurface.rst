Create_Isosurface
=================

Create isosurfaces from a NoiseModelling result table and its associated triangles table.

Overview
--------

``Create_Isosurface.groovy`` creates isosurfaces from a NoiseModelling result table and its associated ``TRIANGLES`` table.

The triangles table must have been created using the ``Receivers/Delaunay_Grid`` WPS block.

The output table is called ``CONTOURING_NOISE_MAP``.

.. figure:: create_isosurface.png
   :align: center
   :alt: Create isosurfaces

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``resultTable``
   Name of the sound levels table generated from ``Noise_level_from_source``.

   Example: ``RECEIVERS_LEVEL``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``isoClass``
   Separation levels in dB for the isosurfaces.

   The first range goes from negative infinity to the first value excluded, then each subsequent range goes from the included value to the next excluded value.

   Default: ``35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0``

   Type: ``String``

``resultTableField``
   Field to read from the result table when generating the isosurface.

   Default: ``LAEQ``

   Type: ``String``

``keepTriangles``
   Whether triangles inside areas of the same iso level are kept.

   Keeping triangles preserves elevation variation within the same iso-level area, but increases output data size. It also significantly reduces computation time.

   Default: ``false``

   Type: ``Boolean``

``smoothCoefficient``
   Polygon smoothing coefficient based on a Bezier-curve smoothing approach.

   If the value is ``0``, smoothing is disabled and polygon altitude is preserved. This can be combined with ``keepTriangles`` to keep altitude variation inside same-level areas.

   Default: ``0``

   Type: ``Double``

Output
------

``result``
   Name of the output table containing the isosurfaces. The table is created in the same schema as the input result table.

   Type: ``String``

Function Signatures
-------------------

The script exposes two entry points:

* ``exec(Connection connection, Map input, ProgressVisitor progressVisitor)``
* ``exec(Connection connection, Map input)``

The second form calls the first one with a ``RootProgressVisitor``.

Execution Notes
---------------

The script comments and inline behavior show the following:

* If ``isoClass`` is not provided, the script uses ``IsoSurface.NF31_133_ISO`` defaults.
* The result table SRID is reused to initialize the isosurface computation.
* ``keepTriangles`` is inverted internally through ``setMergeTriangles``.
* Smoothing is disabled for coefficients below ``0.01``.
* The generated table is created through ``isoSurface.createTable(connection, "IDRECEIVER")``.

