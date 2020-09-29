Noise Map from OSM - Tutorial
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Prerequisites
~~~~~~~~~~~~~~~~~

- You need at least NoiseModelling v.3.0.6, the best is always to use last release.
- If you have just finished the first tutorial, please clean your database with the WPS block *Clean_Database*

.. warning::
   Don't forget to check the *Are you sure* check box before running the process. 

Step 1:  Importing OSM data to the database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Exporting data from openstreetmap.org
------------------------------------------------
* Go to https://www.openstreetmap.org
* Zoom in on the area you want to export
* Export the zone in .osm or .osm.gz format with *Export* button 

.. warning::
   For Mac users, safari may automatically rename the file to "map.osm.xml". Simply delete the ".xml" extension before import.

Import to the database
------------------------------------------------
* Use the WPS block *Import_OSM*

.. note::
   About the Coordinate System (EPSG code)
   
   In several input files, you need to specify coordinates, e.g road network. You can't use the WGS84 coordinates (i.e. GPS coordinates). Acoustic propagation formulas make the assumption that coordinates are metric. Many countries and regions have custom coordinate system defined, optimized for usages in their appropriate areas. It might be best to ask some GIS specialists in your region of interest what the most commonly used local coordinate system is and use that as well for your data. If you don’t have any clue about what coordinate system is used in your region, it might be best to use the Universal Transverse Mercator coordinate system. This coordinate system divides the world into multiple bands, each six degrees width and separated into a northern and southern part, which is called UTM zones (see http://en.wikipedia.org/wiki/UTM_zones#UTM_zone for more details). For each zone, an optimized coordinate system is defined. Choose the UTM zone which covers your region (Wikipedia has a nice map showing the zones) and use its coordinate system.

   Here is the map : https://upload.wikimedia.org/wikipedia/commons/e/ed/Utm-zones.jpg

.. note::
  - Inform the target projection identifier field with the corresponding SRID
  - Enter the path to the file map.osm
  - Select OsmToInputData box then click on the green button



.. warning::
   The current import script from open street map can produce geometries incompatible with NoiseModelling. If an area is a problem try to reduce the area. A much more robust version of this script will be available soon. 
  
Three tables must be created: GROUND, BUILDINGS, ROADS

Step 2: Viewing tables and data layers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Using WPSBuilder
--------------------------------
* The contents of the database can be viewed using *Display_Database*.
* A spatial layer can be visualized using *Table_Visualization_Map*.
* A data table can be visualized using *Table_Visualization_Data*.

Viewing the database
--------------------------------
* **Export a table**
It is also possible to export the tables via *Export_Table* in Shapefile, CSV or GeoJSON format.

* **Viewing a table**
Then import these tables into your preferred Geographic Information System (*e.g.* OrbisGIS, QGIS).
You can then graphically visualize your data layer, but also the data it contains. Take the time to familiarize yourself with your chosen GIS.

* **Adding a background map**
OrbisGIS/QGIS allow you to add an OSM background map : https://wiki.openstreetmap.org/wiki/QGIS

* **Change colors**
OrbisGIS/QGIS allow you to change layer colors (e.g. Surface_osm in green, Buildings_OSM in gray, ROADS in red).

Step 3: Generating a Receiver table
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The locations of noise level evaluation points needs to be defined.

Use *Delaunay_Grid* with the previously generated BUILDINGS table as the buildings table, and ROADS as *Sources table name*.
Other parameters are optional.

Don't forget to view your resulting layer in WPSBuilder or OrbisGIS/QGIS to check that it meets your expectations.

This processing block will give the possibility to generate a noise map later.

Step 4: Using Noise Modelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Associating an emission noise level with roads
------------------------------------------------------------------------------
The *Road_Emission_from_Traffic* block is used to generate a road layer, called LW_ROADS, containing LW emission noise level values in accordance with the emission laws of the CNOSSOS model. The format of the input road layer can be found in the description of the WPS Block.

Don't forget to view your resulting layer in WPSBuilder or OrbisGIS/QGIS to verify that it meets your expectations.

Source to Receiver Propagation
------------------------------------------------------------------------------
The *Noise_level_from_source* block allows to generate a layer of receiver points with associated sound levels corresponding to the sound level emitted by the sources (created table LW_ROADS) propagated to the receivers according to the CNOSSOS propagation laws.

Step 5: Create Isosurfaces map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create an interpolation of levels between receivers points using the block *Create_Isosurface*.

Set *LDEN_GEOM* as *Name of the noise table*.

Step 6: Viewing the result
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Exporting
--------------
You can then export the output table CONTOURING_NOISE_MAP via *Export_Table* in shapefile or GeoJSON format.

Viewing
--------------
You can view this layer in your favorite GIS. You can then apply a color gradient on ISOLVL field, the noise level intervals are in ISOLABEL field.


.. figure:: images/tutorial/Tutorial2_ContouringNoiseMap.png
   :align: center
