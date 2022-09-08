Noise Map from Point Source
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this tutorial, we are going to produce a noise map, based on a unique point source. The exercice will be made through NoiseModelling with Graphic User Interface (GUI).


To make it more simple, we will use the data used in the :doc:`Get_Started_GUI` tutorial.


Step 1: Create the point source
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To create the point source, we will use the free and opensource GIS software `QGIS`_.

.. _QGIS: http://qgis.org/

Load data into QGIS
-------------------------

Once installed, launch QGIS and load the three ``buildings.shp``, ``roads.shp`` and ``ground_type.shp`` files (that are in the folder ``../NoiseModelling_4.0.0/data_dir/data/wpsdata/``). To do so, you can just drag & drop these files into the ``Layers`` menu (bottom left of the user interface). Or you can also select them thanks to the dedicated panel opened via the ``Layer / Add a layer / Add a vectorial layer... /`` menu (or use ``Ctrl+Maj+V`` shortcut)

You should see your input data in the map as below:

.. figure:: images/Noise_Map_From_Point_Source/load_data_qgis.png
   :align: center

|

Initialize the point source layer
----------------------------------

In QGIS, we will create a new empty layer called ``Point_Source`` in which we will add the point source.

To do so, click on the ``Layer / Create Layer / New Temporary Scratch Layer... /`` menu. In the opened dialog, fill the detailed information below :

* ``File name`` : ``Point_Source``
* ``Geometry type`` : choose ``Point`` in the dropdown list
* ``Include Z dimension`` : check the box. This way the created point will be defined with X, Y and Z coordinates
* In the projection system dropdown list, choose a metric system. Here we will choose the one used for the buildings, roads and ground_type layers, that are in metropolitan France : Lambert 93 system = ``EPSG:2154`` (if the system is not present in the dropdown list, use the ``Globe`` icon on the right to find it)

In the ``New field`` part, fill the information below: 

* ``Name`` : PK . Unique id (Primary Key)
* ``Type`` : Integer
* ``Length`` : 2

Once done, click on ``Add to Fields List``. Then redo this step with the following informations:

* ``Name`` : LWD500 . Source noise level (LW) during the day (D) at a frequency of 500 Hz
* ``Type`` : Decimal number
* ``Length`` : 5
* ``Precision`` : 2

You should have something like this

.. figure:: images/Noise_Map_From_Point_Source/create_source_point_layer.png
   :align: center
|

Once done, click on ``OK`` button. The new layer ``Point_Source`` should appear in your ``Layers`` panel.


Add a new point source
-------------------------

Now we have an empty layer. It's time to feed it with a point geometry. 

By default, the new temporary layer is already turned into edtion mode. If not, you can activate it thanks to these two options:

* In the ``Layers`` panel, select the ``Point_Source`` layer and make a right-click. Choose ``Toggle Editing``
* or you can click on the "Yellow pencil" icon in the toolbar

.. figure:: images/Noise_Map_From_Point_Source/edit_layer_source.png
   :align: center

|
Now we can add a new point, by clicking on the dedicated icon (see illustration below) and then by clicking somewhere in the map.

To have an interesting resulting noise map, choose to place your source point next to buildings.

.. figure:: images/Noise_Map_From_Point_Source/place_point_source.png
   :align: center
|

Click on the map where you want to create the point source. Once clicked, a new dialog appears and you are invited to fill the following attributes:

* ``PK``: 1
* ``LWD500`` : 90

.. figure:: images/Noise_Map_From_Point_Source/fill_attributes.png
   :align: center
|

Once done, click on ``OK``. The point source is now visible in the map (the blue point in the illustration below).

.. figure:: images/Noise_Map_From_Point_Source/layer_source.png
   :align: center
|

Now, we have to save this temporary layer into a flat file. To do so, just make a right-click on the layer name and choose the  ``Make permanent`` option.


.. figure:: images/Noise_Map_From_Point_Source/convert_point_source_geojson.png
   :align: center
|

In the new dialog, select ``GeoJSON`` file format and then define the path and the name of your resulting .geojson file. Press ``OK`` when ready.

.. figure:: images/Noise_Map_From_Point_Source/save_geojson.png
   :align: center
|


Your ``Point_Source.geojson`` file is now ready to be imported in NoiseModelling.



Step 2: Import input data in NoiseModelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once NoiseModelling is launched (see ``Step 2: Start NoiseModelling GUI`` in :doc:`Get_Started_GUI` page), load the four ``BUILDINGS``, ``ROADS`` and ``GROUND_TYPE``, ``POINT_SOURCE`` layers (see ``Step 4: Load input files`` for more details).

If you use the ``Database_Manager:Display_Database`` WPS script, you should see your four tables like below:

.. figure:: images/Noise_Map_From_Point_Source/table_list_NM.png
   :align: center
|


Step 3: Generate the noise map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are now ready to generate the noise map, based on a unique source point.

Create the receivers grid
---------------------------

Use the ``Receivers:Delaunay_Grid`` WPS script. Fill the two following mandatory parameters *(in orange)* and click on ``Run Process`` button:

* ``Source table name`` : ``POINT_SOURCE``
* ``Buildings table name`` : ``BUILDINGS``

Once done, you should have two new tables : ``RECEIVERS`` *(illustrated below with the purple small points)* and ``TRIANGLES``

.. figure:: images/Noise_Map_From_Point_Source/table_receivers.png
   :align: center
|

Calculate noise levels
---------------------------

Use the ``NoiseModelling:Noise_level_from_source`` WPS script. Fill the three following mandatory parameters *(in orange)*:

* ``Source table name`` : ``POINT_SOURCE``
* ``Receivers table name`` : ``RECEIVERS``
* ``Buildings table name`` : ``BUILDINGS``


.. warning::
   For this example, since we only added information for noise level during the day (field ``LWD500``), we have to skip the noise level calculation for LDEN, LNIGHT and LEVENING. To do so, check the boxes for ``Do not compute LDEN_GEOM``, ``Do not compute LEVENING_GEOM`` and ``Do not compute LNIGHT_GEOM`` options.

Once ready, click on ``Run Process`` button.

You should then have this message: ``Calculation Done ! LDAY_GEOM table(s) have been created.``

Generate noise level isosurfaces
----------------------------------

Use the ``Acoustic_Tools:Create_Isosurface`` WPS script. Fill the following mandatory parameter *(in orange)* and click on ``Run Process`` button:

* ``Sound levels table`` : ``LDAY_GEOM``

You should have this message: ``Table CONTOURING_NOISE_MAP created``

Now, you can export this table into a .shapefile, using the ``Import_and_Export:Export_Table`` WPS script.

You can then visualize this file into QGIS *(just load the file as seen before)*. The resulting table *(in grey)* is illustred below

.. figure:: images/Noise_Map_From_Point_Source/table_contouring.png
   :align: center
|


Apply a color palette adapted to acoustics
-----------------------------------------------

In QGIS, since the isosurface table is not easy to read *(everything is grey in our example)*, we will change the color palette to have colors depending on the noise levels. This information is present in the field ``ISOLVL`` in the attributes table. To open it, just select the layer ``CONTOURING_NOISE_MAP`` and press ``F6``.

.. figure:: images/Noise_Map_From_Point_Source/contouring.png
   :align: center
|


To adapt the colors, we will apply a cartographic style. This style:

* has been proposed by B. Weninger in *"A Color Scheme for the Presentation of Sound Immission in Maps : Requirements and Principles for Design"* (see `publication`_)
* is provided *(by NoiseModelling team)* as a ``.sld`` *(Style Layer Descriptor)* file and can be downloaded `here`_ 


.. _publication : https://www.semanticscholar.org/paper/A-Color-Scheme-for-the-Presentation-of-Sound-in-%3A-Weninger/a72d13fcc53488567b45a08a78f969c7b3552ac0

.. _here : ./styles/style_noisemap.sld

Once downloaded, make a double click on the layer ``CONTOURING_NOISE_MAP``. It will opens the property panel. Here, click on the ``Symbology`` tab.
In the ``Style`` menu *(at the bottom)*, choose ``Load style``. Then in the opened dialog, click on the ``...`` icon to search the ``style_noisemap.sld`` file. Once selected, click on ``Load style``. 

.. figure:: images/Noise_Map_From_Point_Source/style_sld.png
   :align: center
|


The style with its different colors is now displayed. 

.. figure:: images/Noise_Map_From_Point_Source/style_scale.png
   :align: center
|

Press ``OK`` to apply and close the dialog. Your noise map is now well colorized and you can navigate into it to see the influence of buildings on noise levels.

.. figure:: images/Noise_Map_From_Point_Source/style_map.png
   :align: center
|

Step 4: Change the default parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To produce this noise map, we used, in most of WPS scripts, default parameters (*e.g* the height of the source, the number of reflections, the air temperature, …). You are prompted to redo some of the previous steps by changing some of the settings. You will then be able to visually see what impact they have on the final noise map.

.. note::
   To change optionnal parameters *(the yellow boxes)* just select them and fill the needed informations in the right-side menu.

.. figure:: images/Noise_Map_From_Point_Source/image_15.png
   :align: center
|

Step 5 (bonus): Change the directivity
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


* ``Name`` : YAW . Source horizontal orientation in degrees. For points 0° North, 90° East. For lines 0° line direction, 90° right of the line direction.
* ``Type`` : Decimal number
* ``Length`` : 4

* ``Name`` : PITCH . Source vertical orientation in degrees. 0° front, 90° top, -90° bottom. (FLOAT).
* ``Type`` : Decimal number
* ``Length`` : 4

* ``Name`` : ROLL . Source roll in degrees
* ``Type`` : Decimal number
* ``Length`` : 4

* ``Name`` : DIR_ID . Identifier of the directivity sphere from tableSourceDirectivity parameter or train directivity if not provided -> OMNIDIRECTIONAL(0), ROLLING(1), TRACTIONA(2), TRACTIONB(3), AERODYNAMICA(4), AERODYNAMICB(5), BRIDGE(6)
* ``Type`` : Integer
* ``Length`` : 2

