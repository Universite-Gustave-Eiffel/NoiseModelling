.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Random Grid
===========

Overview
--------

➡️ Computes a random grid of receivers.
✅ The output table is called RECEIVERS

.. figure:: receivers_random_output.png
   :align: center
   :alt: Random grid output

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``buildingTableName`` — *Buildings table name*
   Name of the Buildings table  The table must contain:
   
   *  THE_GEOM: the 2D geometry of the building (POLYGON or MULTIPOLYGON)
   
   *  HEIGHT: the height of the building (FLOAT)

   Type: ``String``

``sourcesTableName`` — *Sources table name*
   Keep only receivers at least at 1 meters of provided sources geometries  The table must contain :
   
   *  THE_GEOM: any geometry type.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``fence`` — *Extent filter*
   Create receivers only in the provided polygon.  Must be in the WGS84 (EPSG:4326) projection system

   Type: ``Geometry``

``fenceTableName`` — *Filter using table bounding box*
   Extract the bounding box of the specified table then create only receivers on the table bounding box.  The table must contain :
   
   *  THE_GEOM: any geometry type.

   Type: ``String``

``height`` — *Height*
   Height of receivers (in meters) (FLOAT)

   Type: ``Double``

   Default: ``4``

``nReceivers`` — *Number of receivers*
   Number of receivers to return
   
   .. figure:: receivers_random_nReceivers.png
      :align: center
      :alt: Number of receivers

   Type: ``Integer``

   Default: ``100``

Output
------

``result`` — *Created table*
   Name of the table containing the results of the computation. Can be used as input for another process.

   Type: ``String``

