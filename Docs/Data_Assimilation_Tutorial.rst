Data Assimilation
^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~

Data assimilation is a technique that combines observations with a numerical model to improve the accuracy of forecasts or analyses. Applied to acoustics and noise maps, data assimilation makes it possible to integrate real noise measurements (*e.g* coming from sensors) into noise propagation models to produce more accurate and reliable noise maps.

In this tutorial, we will see how to model and simulate noise in `Geneva`_ 🇨🇭 using static (road network, buildings) and dynamic (traffic, temperature) data.
The main objective is to **combine measurements from sensors** located in Geneva **with acoustic simulations** to **identify traffic configurations that best match measurements**.

.. _Geneva: https://www.openstreetmap.org/relation/1685488

Requirements
~~~~~~~~~~~~~~~~~

To play with this tutorial, you will need:

* a working installation of NoiseModelling (NM) with at least version 5.0.1. If needed, get the last release on the `GitHub repo`_.
* the tutorial's datasets, stored in the folder ``.../NoiseModelling_5.0.1/data_dir/data/wpsdata/dataAssimilation/``:
    - ``devices_data``: the folder containing sensor measurement files in ``.csv`` format. Each file represents the measurements of a single sensor.
    - ``device_mapping_sf.geojson`` containing the geometry and unique identifier of all sensors. This file is also provided in ``.csv`` format.
    - the `OpenStreetMap`_ (OSM) dataset of the studied area : ``geneva.osm.pbf``.

.. _OpenStreetMap: https://www.openstreetmap.org/
.. _GitHub repo: https://github.com/Universite-Gustave-Eiffel/NoiseModelling/releases

The data
~~~~~~~~~~~~~~~

Each ``.csv`` files in the folder ``devices_data`` contains environmental noise measurements captured by individual sensors.
The columns are:

* ``deveui`` : Unique identifier of the sensor,
* ``epoch`` : Time of measurement (Epoch format - Unix time, ex:1724567400),
* ``Leq`` : Equivalent continuous sound level in dB(A), calculated over a period (15 min),
* ``Temp`` : Temperature (°C) recorded by the sensor at the time of measurement,
* ``timestamp`` : Time of measurement (timestamp format ``"YYYY-MM-DD HH:MM:SS"``).

Below is an illustration with the file ``4a6.csv`` 

.. csv-table:: Informations stored in the sensor's file ``4a6.csv``
   :file: ./data/4a6.csv
   :widths: 15, 15, 15, 15, 40
   :header-rows: 1

All values are sampled approximately every 15 minutes, but the exact spacing may vary slightly.
Timestamps should therefore be aligned at fixed 15-minute intervals.

The ``device_mapping_sf.geojson`` columns are:

* ``deveui`` : Unique Identifier of the sensor,
* ``the_geom`` : 3D point geometry in WKT (Well-Known Text) format — includes coordinates (X, Y) and altitude (Z) in the projected coordinate system.

Below is a map, showing the seven sensors (red points), with their identifier ``deveui``.

.. image:: ./images/Data_Assimilation/geneva_sensors.png
    :alt: Sensors in Geneva


Data Simulation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This process prepares the training dataset from sensor measurements, importing it into NoiseModelling (in a spatial database) and performing various calculations to determine noise levels.

Maps are generated with all possible combinations, in order to identify the best road configuration in terms of noise levels generated.

Step 1 : Generate all possible combinations
-------------------------------------------------

Generates all possible combinations of values from two given lists and inserts them into a table named ``ALL_CONFIGURATIONS``.

The generated combinations include:

* ``trafficValues``:  variation around standard values for the 4 types of roads: primary, secondary, tertiary and others. When generating a variation of the default map (the set of maps), the values taken by the primary, the secondary, ... sections, will be calculated from these parameters (between 1 and *n*). For example, if ``"trafficValues": [0.01, 1]``, then the primary sections will take 1% or 100% of their default value. The same applies to the remaining road types.
* ``temperatureValues`` : the temperatures (in °C - Integer). The 1 to *n* possible temperatures to play

 .. code-block:: groovy

    new All_Possible_Configuration().exec(connection,[
                   "trafficValues": [0.01, 1.0, 2.0],
                   "temperatureValues": [10,15,20]
    ])

To calculate the combinations, you have to execute the WPS .groovy file ``All_Possible_Configuration.groovy`` stored in the folder ``NoiseModelling_5.0.0/data_dir/scripts/wps/DataAssimilation/``.


The generated combinations include values for type of roads primary, secondary, tertiary, others, and temperature. The resulting table ``ALL_CONFIGURATIONS`` has the following columns:

* ``IT``: Unique identifier of the combination (Primary Key - Integer)
* ``PRIMARY_VAL``: percentage of primary roads traffic, given by trafficValues  (Double)
* ``SECONDARY_VAL``: (Double)
* ``TERTIARY_VAL``: (Double)
* ``OTHERS_VAL``: (Double)
* ``TEMP_VAL``: (Integer)

.. warning::
    The total number of combinations can be huge. This value is defined as: number of ``trafficValues`` elements ^ 4  * (number of ``temperatureValues`` elements). In our example ``"trafficValues": [0.01, 1.0, 2.0]`` and ``"temperatureValues": [10,15,20]`` gives 3 ^ 4 * 3 = 243 combinations.

    Even though this table is very important, only part of it will be used for all the maps to be simulated (see Step 5).


Step 2 : Import sensor positions
---------------------------------------

Importing the location of the sensors into the NoiseModelling's database from the .geojson file ``device_mapping_sf.geojson``. Since we are in the Geneva area, we are using the ``CH1903+`` metric coordinate system (identified as `EPSG:2056`_).

.. code-block:: groovy

    new Import_File().exec(connection,[
                    "pathFile" : workingFolder+"device_mapping_sf.geojson",
                    "inputSRID" : 2056,
                    "tableName": "SENSORS_LOCATION"
    ])

.. _EPSG:2056: https://epsg.io/2056


Step 3 : Prepare sensor data
---------------------------------------

This part extracts and prepare sensors, for a given period, based on the following parameters:  

* ``startDate`` : the start timestamp to extract the dataset (in format ``YYYY-MM-DD HH:MM:SS``)
* ``endDate`` : the start timestamp to extract the dataset (in format ``YYYY-MM-DD HH:MM:SS``)
* ``trainingRatio`` : define the percentage of data to be used for training (Double)
* ``workingFolder`` : folder containing the file ``device_mapping_sf.csv``, the OSM file and the ``devices_data`` folder. So in our case ``"workingFolder": dataAssimilation``
* ``targetSRID``: Target projection identifier (also called SRID) of your project. So in our case ``2056`` (Integer)

.. code-block:: groovy

    new Prepare_Sensors().exec(connection,[
                    "startDate":"2024-08-25 06:30:00",
                    "endDate": "2024-08-25 07:30:00",
                    "trainingRatio": 0.8,
                    "workingFolder": workingFolder,
                    "targetSRID": 2056
    ])

To prepare the sensors, you have to execute the WPS .groovy file ``Prepare_Sensors.groovy`` stored in the folder ``NoiseModelling_5.0.0/data_dir/scripts/wps/DataAssimilation/``.

Once done, two tables are created:

* ``SENSORS_MEASUREMENTS`` : representing all the data for this period
* ``SENSORS_MEASUREMENTS_TRAINING`` : training data set representing the receiver

Step 4: Import buildings and roads
---------------------------------------

Import buildings and road network (with predicted traffic flows) from the ``geneva.osm.pbf`` OSM file.

.. code-block:: groovy

    new Import_OSM().exec(connection, [
                    "pathFile"      : workingFolder+"geneva.osm.pbf",
                    "targetSRID"    : 2056,
                    "ignoreGround"  : true,
                    "ignoreBuilding": false,
                    "ignoreRoads"   : false,
                    "removeTunnels" : true
    ])

Step 5 : Generate all traffic emissions and maps
-----------------------------------------------------

This step generates all traffic emission by modifying traffic data according to the road type, using data from ``ALL_CONFIGURATIONS``.
The optional ``noiseMapLimit`` parameter limits the number of maps to be generated.

The ``LW_ROADS`` table containing all traffic emission and ``ROADS_GEOM`` table containing the geometry of roads are created.

.. code-block:: groovy

    new DataSimulation().exec(connection,[
                    "noiseMapLimit": 80
    ])

Compute the attenuation noise level from the network sources emission (``LW_ROADS``) to the receivers. The ``RECEIVERS_LEVEL`` table represents the table of all generated maps.

.. code-block:: groovy

    new Noise_level_from_source().exec(connection, [
                    "tableSources": "ROADS_GEOM",
                    "tableSourcesEmission" : "LW_ROADS",
                    "tableBuilding": "BUILDINGS",
                    "tableReceivers": "SENSORS_LOCATION",
                    "confExportSourceId": false,
                    "confMaxSrcDist": 250,
                    "confDiffVertical": false,
                    "confDiffHorizontal": false
    ])

Step 6 : Extract best configuration
---------------------------------------

Many maps have been generated. So now, the best map, **minimizing the difference between the measurements and the simulation**, must be chosen.

By considering a tolerance threshold (``tempToleranceThreshold`` - exprimed in °C) for the temperature that allows to extract the map that have a temperature value close to the real temperature. And then, by calculating the LAEQ difference between simulated (``RECEIVERS_LEVEL`` data) and observed (``SENSORS_MEASUREMENTS_TRAINING`` data) values.

For each time steps, the median value of the difference between the two values is calculated, for all maps. **The map having the smallest median value will be the best one**.

.. code-block:: groovy

    new Extract_Best_Configuration().exec(connection,[
                    "observationTable": "SENSORS_MEASUREMENTS_TRAINING",
                    "noiseMapTable": "RECEIVERS_LEVEL",
                    "tempToleranceThreshold"  : 5
    ])

.. note::
    The best configuration is found for each time step (here 15 minutes)

As a result, the ``BEST_CONFIGURATION_FULL`` table is created.


Execute Simulation: Generate the Dynamic Map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
This part is designed to execute a dynamic traffic calibration process, using the best configuration.

Step 7 : Generate new receivers
---------------------------------

Create a regular grid of receivers (here 1 every 200m) between the buildings.

.. code-block:: groovy

    new Regular_Grid().exec(connection,[
                  "fenceTableName": "BUILDINGS",
                  "buildingTableName": "BUILDINGS",
                  "sourcesTableName":"ROADS",
                  "delta": 200
    ])

The table ``RECEIVERS`` is created.

Step 8 : Adding sensors as receivers
---------------------------------------

**Optionally**, add the sensors into the ``RECEIVERS`` after creating a regular grid of receivers.

.. code-block:: groovy

    new Merged_Sensors_Receivers().exec(connection,[
                    "tableReceivers": "RECEIVERS",
                    "tableSensors" : "SENSORS_LOCATION"
    ])

Step 9 : Generate dynamic road emissions
------------------------------------------

For each time step (here 15 min), generate an emissions map for all the receivers, corresponding to the best configuration (for this time step).




.. code-block:: groovy

    new NMs_4_BestConfigs().exec(connection)

The table ``LW_ROADS_best`` is created.


Step 10 : Generate the noise levels
---------------------------------------

Compute the noise level from the network sources emission (``LW_ROADS_best``) based on all the receivers.

.. code-block:: groovy

    new Noise_level_from_source().exec(connection, [
                          "tableSources": "ROADS_GEOM",
                          "tableSourcesEmission" : "LW_ROADS_best",
                          "tableBuilding": "BUILDINGS",
                          "tableReceivers": "RECEIVERS",
                          "confExportSourceId": false,
                          "confMaxSrcDist": 250,
                          "confDiffVertical": false,
                          "confDiffHorizontal": false
    ])

Step 11 : Create & visualize the resulting table
--------------------------------------------------

Create a table, called ``ASSIMILATED_MAPS``, containing both sound levels and configuration parameters

.. code-block:: groovy

    new Create_Assimilated_Maps().exec(connection,[
                    "bestConfigTable" : "BEST_CONFIGURATION_FULL",
                    "receiverLevel" : "RECEIVERS_LEVEL",
                    "outputTable": "ASSIMILATED_MAPS"
    ])

You can now export ``ASSIMILATED_MAPS``, for example as a Shapefile and then import it into your favorite GIS app (such as `QGIS`_) to visualize the results.

.. code-block:: groovy

    new Export_Table().exec(connection,
                    ["exportPath": workingFolder+"results/ASSIMILATED_MAPS.shp",
                     "tableToExport": "ASSIMILATED_MAPS"
    ])


.. _QGIS: https://qgis.org/