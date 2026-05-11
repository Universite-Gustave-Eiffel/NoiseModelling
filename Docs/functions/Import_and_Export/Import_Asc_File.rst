Import_Asc_File
===============

Import an ESRI ASCII raster file as a DEM.

Overview
--------

``Import_Asc_File.groovy`` imports an ESRI ASCII raster file and converts it into a DEM compatible with NoiseModelling.

The output table is ``DEM`` and contains:

* ``THE_GEOM``: 3D DEM point cloud as points

.. figure:: import_asc_file.png
   :align: center
   :alt: Import asc file

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile``
   Path to the input ``.asc`` or ``.asc.gz`` file.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   Projection identifier of the input ASCII file.

   Default: ``4326``

   Type: ``Integer``

``fence``
   Polygon geometry used to restrict DEM extraction to a subset area.

   Type: ``Geometry``

``downscale``
   Number of pixels skipped on each axis.

   Default: ``1``

   Type: ``Integer``

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

* It accepts ``.asc`` and ``.asc.gz`` files only.
* If a matching ``.prj`` file exists, the SRID is inferred from it; otherwise the default or provided SRID is used.
* If a fence is provided, it is transformed to the DEM SRID before extraction.
* It imports the raster as 3D points and creates a spatial index on ``DEM``.

