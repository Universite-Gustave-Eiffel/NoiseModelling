Get Started - Tutorial
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Requirements: Install Java
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please install JAVA > v8.0
- https://www.java.com/fr/download/

Check if JAVA_HOME environnement variable is well setted to your last installed java folder.

Step 1: Download the lastest realese
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Download the latest realease on `Github`_. 
- Unzip the downloaded file into your working directory.

.. note::
    Only from version 3.0, NoiseModelling releases include a user interface.

.. _Github : https://github.com/Ifsttar/NoiseModelling/releases

Step 2: Run GeoServer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling connects to a PostGIS or H2GIS database. The database needs to be hosted by a server. 
In this tutorial the server type is `GeoServer`_ and the database type is `H2GIS`_. 

To run the server, please execute "startup" from your own Geoserver folder :

- Geoserver\\bin\\startup.bat for Windows Users
- Geoserver\\bin\\startup.sh for Linux Users

and wait until :literal:`INFO:oejs.Server:main:Started` is written in your command prompt.


Your local server is now started. 

.. tip::
    You can consult it via your web browser : http://localhost:8080/geoserver/web/
    login (default): admin
    password (default): admin

.. _GeoServer : http://geoserver.org/
.. _H2GIS : http://www.h2gis.org/

Step 3: Run WPSBuilder
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The WPSBuilder is the user interface used to communicate between the GeoServer and NoiseModelling.

To launch WPSBuilder, please run:

- WPSBuilder\\index.html

Step 4: Upload files to database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To compute your first noise map, you will need 5 layers: Buildings, Roads, Ground type, Topography (DEM) and Receivers.

In the Geoserver\\data_dir\\data\\wpsdata folder, you will find 5 files (4 shapefile and 1 geojson) corresponding to these layers.

You can import these layers in your database using the *Import File* or *Import Folder* blocks.

- Drag *Import File* block into Builder window 
- Select *Path of the input File* block and add your local pathFile in the Inputs windows. 
- Then, click on *Run Process*

.. figure:: images/tutorial/Tutorial1_Image1.PNG
   :align: center

Files are uploaded to database when the Console window displays :literal:`The table x has been upload to database.`

Repeat it 5 times, one for each file.

.. note::
    - The process is suppose to be quick (<5 sec.). In case of out of time, try to restart the Geoserver (see Step 2).
    - Orange blocks are mandatory
    - Beige blocks are optional
    - Blocks get solid border when they are ready to run

Step 5: Run Calculation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run Calculation you have to drag the block *Get Lday* into WPS Builder window.

Then, select the orange blocks and indicate the name of the corresponding table your database, for example :
- Building table name : "BUILDING"
- Sources table name : "ROADS"
- Receivers table name : "RECEIVERS"

Then, you can run the process.

.. figure:: images/tutorial/Tutorial1_Image2.PNG
   :align: center

The table LDAY_GEOM will be created in your database.

.. tip::
    if you want you can try to change the different parameters.


Step 6: Export (& see) the results
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can now export the table LDAY_GEOM in your favorite export format using *Export Table* block.

.. figure:: images/tutorial/Tutorial1_Image3.PNG
   :align: center

For example, you can choose to export the table in shp format. This format can be read with many GIS tools such as the open source software Qgis.

.. figure:: images/tutorial/Tutorial1_Image4.PNG
   :align: center