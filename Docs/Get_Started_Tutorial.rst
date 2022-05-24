Get Started - Tutorial
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Requirements: 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Windows
-------------------

Since version 3.3.2, an executable file has been made for you ! You can go directly to step 1.

Other platforms
---------------------------------

Please install JAVA version v8.x. Currently only version 8 of Java is compatible

- Download Java here : https://www.java.com/fr/download/

- You can check if JAVA_HOME environnement variable is well settled to your last installed java folder using ``echo %JAVA_HOME%`` (windows) or ``echo $JAVA_HOME`` (linux) in your command prompt. You should have a result similar to ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\``

-  If you don't have this result, it is probably because your JAVA_HOME environnement variable is not well settled. To set you JAVA_HOME environnement variable you can adapt (with ``x`` the JAVA version number) you installed and use the following command line : ``setx JAVA_HOME  "C:\\Program Files (x86)\\Java\\jre.1.8.x_x"`` in your command prompt. You can also refer to `this document`_ for example. 

- You may have to reboot your command prompt after using the precedent command line before printing again ``echo %JAVA_HOME%`` (windows) or ``echo $JAVA_HOME`` (linux).

.. warning::
    The command promprt should print ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\`` whithout the bin directory. If JAVA_HOME is settled as ``C:\\Program Files (x86)\\Java\\jre1.8.x_x\\bin``, it will not work. It should also point to a JRE  (Java Runtime Environment) Java environnement and not JDK. 
    
.. _this document : https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html   

Docker Setup
---------------------------------

When a developer uses Docker (https://www.docker.com/), he creates an application or service, which he then bundles together with the associated dependencies in a container image. An image is a static representation of the application or service, its configuration and dependencies.

A docker image for the NoiseModelling v3.4.4 library has already been built. Please visit: https://github.com/tomasanda/docker-noisemodelling

.. warning::
    This docker version is made with NoiseModelling v3.4.4 which is an old release. A news Docker version with the last NoiseModelling version may be made. 

Step 1: Download the latest release
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Download the latest realease of NoiseModelling on `Github`_. 
- *Windows* : you can directly download the executable install file and launch it. 
- *Other plaforms* : Unzip the downloaded zip file into a chosen directory.

.. warning::
    The chosen directory can be anywhere, but be sure that you have write access. If you are using the computer of your company, the Program Files folder is probably not a good idea.

.. note::
    - Only from version 3.3, NoiseModelling releases include the user interface described in this tutorial. 

.. _Github : https://github.com/Ifsttar/NoiseModelling/releases

Step 2: Run GeoServer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling connects to a PostGIS or H2GIS database. The database needs to be hosted by a server. 
In this tutorial the server type is `GeoServer`_ and the database type is `H2GIS`_. 
Those tools are included in the archive.

To run the server, please execute ``startup`` from your own Geoserver folder :

- ``NoiseModelling.exe`` for Windows Users 
- ``NoiseModelling\\bin\\startup.sh`` for Linux and Mac Users (check authorize file execution in property of this file before)

and wait until ``INFO:oejs.Server:main:Started`` is written in your command prompt.


.. warning::
    The server launch can take some time. Be patient.

Your local server is now started. 

.. warning::
    Your server will be open as long as the command window is open. If you close it, the server will automatically be closed and you will not be able to continue with the tutorial.

.. tip::
    You can consult it via your web browser : http://localhost:9580/geoserver/web/
    
    - **login (default):** admin
    - **password (default):** admin

.. warning::
    On older versions, the url was: http://localhost:8080/geoserver/web/ 

.. _GeoServer : http://geoserver.org/
.. _H2GIS : http://www.h2gis.org/

Step 3: Run WPSBuilder
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The WPSBuilder is the user interface used to communicate between the GeoServer and NoiseModelling.

To launch WPSBuilder, go to http://localhost:9580 using your preferred web browser.

Step 4: Upload files to database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To compute your first noise map, you will need 5 layers: Buildings, Roads, Ground type, Topography (DEM) and Receivers.

In the ``noisemodelling\\data_dir\\data\\wpsdata`` folder, you will find 5 files (4 shapefiles and 1 geojson) corresponding to these layers.

You can import these layers in your database using the ``Import File`` or ``Import Folder`` blocks.

- Drag ``Import File`` block into the Builder window 
- Select ``Path of the input File`` box and type ``data_dir/data/wpsdata/buildings.shp`` in the field ``PathFile`` *(on the right-side column)*
- Then click on ``Run Process`` after selecting one of the sub-boxes of your process

.. figure:: images/tutorial/Tutorial1_Image1bis.gif
   :align: center

Files are uploaded to database when the Console window displays ``The table x has been uploaded to database``.

Repeat this operation for other files:

- ``data_dir/data/wpsdata/ground_type.shp``
- ``data_dir/data/wpsdata/receivers.shp``
- ``data_dir/data/wpsdata/ROADS2.shp``
- ``data_dir/data/wpsdata/dem.geojson``

.. note::   
    You can find all files in your own NoiseModelling folder, at direction ``/data_dir/data/wpsdata/``

.. note::
    - if you have the message ``Error opening database``, please refer to the note in Step 1.
    - The process is supposed to be quick (<5 sec.). In case of out of time, try to restart the Geoserver (see Step 2).
    - Orange blocks are mandatory
    - Beige blocks are optional
    - if all input blocks are optional, you must modify at least one of these blocks to be able to run the process
    - Blocks get solid border when they are ready to run
    - Find `here`_ more information about `WPS Builder`_.

.. _here : https://noisemodelling.readthedocs.io/en/latest/FAQ_UF.html

.. _WPS Builder : https://noisemodelling.readthedocs.io/en/latest/FAQ_UF.html

Step 5: Run Calculation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run Calculation you have to drag the block ``Noise_level_from_traffic`` into WPS Builder window.

Then, select the orange blocks and indicate the name of the corresponding table your database, for example :

- Building table name : ``BUILDINGS``
- Sources table name : ``ROADS2``
- Receivers table name : ``RECEIVERS``

Then, you can run the process.

.. figure:: images/tutorial/Tutorial1_Image2bis.PNG
   :align: center

The tables ``LDAY_GEOM``, ``LEVENING_GEOM``, ``LNIGHT_GEOM`` and ``LDEN_GEOM`` will be created in your database.

.. note::
    If you want to know more about the format of the input tables, you can refer to the `WPS Blocks`_ section.

.. tip::
    If you want you can try to change the different parameters.

.. _WPS Blocks : WPS_Blocks.html

Step 6: Export (& see) the results
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can now export the output table in your favorite export format using *Export Table* block giving the path of the file you want to create.

.. warning::
    Dont' forget to add the file extension (*e.g* ``c:/home/receivers.geojson`` or ``c:/home/lday_geom.shp``) (Read more info about file extensions here: :doc:`Tutorials_FAQ`)

.. figure:: images/tutorial/Tutorial1_Image3.PNG
   :align: center

For example, you can choose to export the table in ``.shp`` format. This format can be read with many GIS tools such as the open source softwares `QGIS`_ and `SAGA`_.

To obtain the following image, use the syling vector options in your GIS and assign a color gradient to LAEQ column of your exported ``LDAY_GEOM`` table.

.. figure:: images/tutorial/Tutorial1_Image4.PNG
   :align: center

.. _QGIS : https://www.qgis.org/fr/site/
.. _SAGA : http://www.saga-gis.org/en/index.html


Step 7: Know the possibilities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now that you have finished this first step, take the time to read the description of each of the WPS blocks present in your version of NoiseModelling.

By clicking on each of the inputs or outputs, you can also get additional information.

.. figure:: images/tutorial/Tutorial1_ImageLast.gif
   :align: center

