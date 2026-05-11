.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

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

``buildingTableName``
   Name of the Buildings table  The table must contain:
   
   *  THE_GEOM: the 2D geometry of the building (POLYGON or MULTIPOLYGON)
   
   *  HEIGHT: the height of the building (FLOAT)

``sourcesTableName``
   Keep only receivers at least at 1 meters of provided sources geometries  The table must contain :
   
   *  THE_GEOM: any geometry type.

``nReceivers``
   Number of receivers to return
   
   .. figure:: receivers_random_nReceivers.png
      :align: center
      :alt: Number of receivers

   Default: ``100``

Optional inputs
~~~~~~~~~~~~~~~

``height``
   Height of receivers (in meters) (FLOAT)

   Default: ``4``

``fence``
   Create receivers only in the provided polygon.  Must be in the WGS84 (EPSG:4326) projection system

``fenceTableName``
   Extract the bounding box of the specified table then create only receivers on the table bounding box.  The table must contain :
   
   *  THE_GEOM: any geometry type.

Output
------

``result``
   Name of the table containing the results of the computation. Can be used as input for another process.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
