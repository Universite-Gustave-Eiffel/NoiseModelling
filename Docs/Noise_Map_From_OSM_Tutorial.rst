Noise Map from OSM - Tutorial
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Prerequisites
~~~~~~~~~~~~~~~~~

You need at least NoiseModelling v.3.0.4

Step 1:  Importing OSM data to the database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Exporting data from openstreetmap.org
------------------------------------------------
* Go to https://www.openstreetmap.org
* Zoom in on the area you want to export
* Export the zone in .osm or .osm.gz format

Import to the database
------------------------------------------------
* Use the WPS block *Get_Table_from_OSM*

.. note ::
  Inform the Projection identifier field with a metric SRID
  Enter the path to the file map.osm

Three tables must be created: SURFACE_OSM, BUILDINGS_OSM, ROADS

Step 2: Viewing tables and data layers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Using WPSBuilder
--------------------------------
* The contents of the database can be viewed using *Display_Database*.
* A spatial layer can be visualized using *Table_Visualization*.

Viewing the database
--------------------------------
* **Export a table**
It is also possible to export the tables via *Export_Table* in shapefile or GeoJSON format.

* **Viewing a table**
Then import these tables into your preferred Geographic Information System (*e.g.* OrbisGIS, QQIS).
You can then graphically visualize your data layer, but also the data it contains. Take the time to familiarize yourself with your chosen GIS.

* **Adding a background map**
OrbisGIS/QGIS allow you to add an OSM background map.

* **Change colors**
OrbisGIS/QGIS allow you to change layer colors (e.g. Surface_osm in green, Buildings_OSM in gray, ROADS in red).

Step 3: Generating a Receiver Group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Generating the Receiver Grid
---------------------------------------
Use *Regular_Grid with* a spacing of 50 m between the receivers.
Don't forget to view your resulting layer in WPSBuilder or OrsbisGIS/QGIS to check that it meets your expectations.

Step 4: Using Noise Modelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Associating an emission noise level with roads
------------------------------------------------------------------------------
The *Road_Emission_From_AADF* block is used to generate - from a ROADS layer containing an AADF (Estimated Annual average daily flows) column such as the one from OSM - A road layer, called LW_ROADS, containing LW emission noise level values in accordance with the emission laws of the CNOSSOS model.
Don't forget to view your resulting layer in WPSBuilder or OrsbisGIS/QGIS to verify that it meets your expectations.

Source to Receiver Propagation
------------------------------------------------------------------------------
The *Lden_from_Emission* block allows to generate a layer of receiver points with associated sound levels corresponding to the sound level emitted by the sources propagated to the receivers according to the CNOSSOS propagation laws.

Step 5: Viewing the result
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Exporting
--------------
You can then export the output table LDEN_GEOM via *Export_Table* in shapefile or GeoJSON format.

Viewing
--------------
You can view this layer in your favorite GIS. You can then apply a color gradient to your receiver points based on sound levels.
