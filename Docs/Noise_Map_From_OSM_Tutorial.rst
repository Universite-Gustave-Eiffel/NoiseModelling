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
* Use the WPS block *OsmToInputData*

.. note::
  - Noise Modelling use metric projections, please identifies a local projection for your area of interest and its SRID
  - Inform the target projection identifier field with the corresponding SRID
  - Enter the path to the file map.osm
  - Check all check box 

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

Step 3: Generating a Receiver Grid
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use *Regular_Grid* with a distance between the receivers of 50 m (offset).
Use the previously generated BUILDINGS table as the buildings table.
Other parameters are optional.

Don't forget to view your resulting layer in WPSBuilder or OrsbisGIS/QGIS to check that it meets your expectations.

Step 4: Using Noise Modelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Associating an emission noise level with roads
------------------------------------------------------------------------------
The *Road_Emission_from_Traffic* block is used to generate a road layer, called LW_ROADS, containing LW emission noise level values in accordance with the emission laws of the CNOSSOS model. The format of the input road layer can be found in the description of the WPS Bloc.

Don't forget to view your resulting layer in WPSBuilder or OrsbisGIS/QGIS to verify that it meets your expectations.

Source to Receiver Propagation
------------------------------------------------------------------------------
The *Lden_from_Road_Emission* block allows to generate a layer of receiver points with associated sound levels corresponding to the sound level emitted by the sources (created table LW_ROADS) propagated to the receivers according to the CNOSSOS propagation laws.

Step 5: Viewing the result
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add Global Leq Value
----------------------
You can use *Add_Laeq_Leq_columns* to add global Leq and LAeq columns to your output table LDEN_GEOM.

Exporting
--------------
You can then export the output table LDEN_GEOM via *Export_Table* in shapefile or GeoJSON format.

Viewing
--------------
You can view this layer in your favorite GIS. You can then apply a color gradient to your receiver points based on sound levels.
