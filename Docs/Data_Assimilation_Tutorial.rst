Data Assimilation
^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~

In this tutorial, we’ll explore how to model and simulated noise in Geneva using static (road network, buildings) and dynamic (traffic, temperature) data.
The main objective is to combine measurements from sensors located in Geneva with acoustic simulations to identify traffic configurations that best match measurements.

Prerequisites
~~~~~~~~~~~~~~~~~

- You need to have a working installation of NoiseModelling (NM) with version 5.
- A folder named ``devices_data`` containing sensor measurement files in ``CSV`` format. Each file represents the measurements of a single sensor.
- A ``geojson`` file named ``device_mapping_sf.geojson`` containing the geometry and unique identifier of all sensors. This file can also be in csv format.
- The the OpenStreetMap (OSM) data of the given area : ``geneva.osm.pbf``.

The data
~~~~~~~~~~~~~~~

Each CSV file in the folder ``devices_data`` contains environmental noise measurements captured by individual sensors.
The key columns are:
  * ``deveui`` : Unique identifier of the sensor.
  * ``epoch`` : Timestamp in epoch format (Unix time, ex:1724567400), representing the time of measurement.
  * ``timestamp`` : Time in format <b>"%Y-%m-%d %H:%M:%S"</b>, representing the time of measurement.
  * ``Leq`` : Equivalent continuous sound level in dB(A), calculated over a period (15 min).
  * ``Temp`` : Temperature (°C) recorded by the sensor at the time of measurement.
The ``device_mapping_sf.geojson`` columns kay are:
  * ``deveui`` : Unique identifier of the sensor.
  * ``The_GEOM`` : 3D point geometry in WKT (Well-Known Text) format — includes coordinates (X, Y) and altitude (Z) in the projected coordinate system.

All values are sampled approximately every 15 minutes, but the exact spacing may vary slightly.
Timestamps should therefore be aligned at fixed 15-minute intervals.

Data Simulation
-----------------
This process prepares a training dataset from sensor measurements, importing it into a spatial database, and performing various calculations to determine noise levels.
Maps are generated with all possible combinations, in order to identify the best road configuration in terms of noise levels generated.

Step 1 : Generate all possible Configurations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Generates all possible combinations of values from two given lists and inserts them into a sql table named ``ALL_CONFIGURATIONS``
The generated combinations include variation (%) around standard values for type of roads primary, secondary, tertiary, others, and temperature.

.. code-block:: groovy
    new All_Possible_Configuration().exec(connection,[
                   "trafficValues": [0.01,1.0, 2.0,3],
                   "temperatureValues": [10,15,20]
    ])

Step 2 : Import Sensor Positions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Importing the location of the sensors into the database from the csv file ``device_mapping_sf.geojson``.

.. code-block:: groovy
    new Import_File().exec(connection,[
                    "pathFile" : workingFolder+"device_mapping_sf.geojson",
                    "inputSRID" : 2056,
                    "tableName": "SENSORS_LOCATION"
    ])


Step 3 : Preparation of Sensor data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This part extracts sensor data for a given period.Then use ``trainingRation`` (%) for the training data which will represent the receivers.
So at the end of this part, the ``SENSORS_MEASUREMENTS`` (representing all the data for this period), ``SENSORS_MEASUREMENTS_TRAINING`` (training data set representing the receiver) sql tables will be created.

.. code-block:: groovy
    new Prepare_Sensors().exec(connection,[
                    "startDate":"2024-08-25 06:30:00",
                    "endDate": "2024-08-25 07:30:00",
                    "trainingRatio": 0.8,
                    "workingFolder": workingFolder,
                    "targetSRID": 2056
    ])

Step 4: Import Buildings and Roads
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Import buildings and road network (with predicted traffic flows) from an OSM file.

.. code-block:: groovy
    new Import_OSM().exec(connection, [
                    "pathFile"      : workingFolder+"geneva.osm.pbf",
                    "targetSRID"    : 2056,
                    "ignoreGround"  : true,
                    "ignoreBuilding": false,
                    "ignoreRoads"   : false,
                    "removeTunnels" : true
    ])

Step 5 : Generate all Traffic Emissions and Maps
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This step generates all traffic emission by modifying traffic data according to the road type, using data from ``ALL_CONFIGURATIONS``.
The optional ``noiseMapLimit`` parameter limits the number of maps to be generated, in order to avoid ``Out-Of-Memory ``errors.
The ``LW_ROADS`` table containing all traffic emission and ``ROADS_GEOM`` table containing the geometry of roads are created.

.. code-block:: groovy
    new DataSimulation().exec(connection,[
                    "noiseMapLimit": 80
    ])

Compute the attenuation noise level from the network sources emission (LW_ROADS) to the receivers. The ``RECEIVERS_LEVEL`` table represents the table of all generated maps.

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

Step 6 : Extract best Configurations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Many maps have been generated, so the best map,the one that minimizes the difference between the measurements and the simulation, must be chosen.
By considering a tolerance threshold for the temperature that allows to extract the map that have a temperature value close to the real temperature. And then, by calculating the difference in LAEQ between simulated (<b>RECEIVERS_LEVEL data</b>) and observed (<b>SENSORS_MEASUREMENTS_TRAINING data</b>) values.
For each time step, the median value of the difference between the two values for all maps is calculated, and the map corresponding to the smallest median value will be the best map.
At the end the ``BEST_CONFIGURATION_FULL`` table is created.

.. code-block:: groovy
    new Extract_Best_Configuration().exec(connection,[
                    "observationTable": "SENSORS_MEASUREMENTS_TRAINING",
                    "noiseMapTable": "RECEIVERS_LEVEL",
                    "tempToleranceThreshold"  : 5
    ])

Execute Simulation: Generate the Dynamic Map
-----------------
This pars is designed to execute a dynamic traffic calibration process using the best configuration.

Step 7 : Generate new Receivers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create a regular grid between the buildings of over 4000 receivers.

.. code-block:: groovy
    new Regular_Grid().exec(connection,[
                  "fenceTableName": "BUILDINGS",
                  "buildingTableName": "BUILDINGS",
                  "sourcesTableName":"ROADS",
                  "delta": 200
    ])


Step 8 : Adding Sensors as Receivers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Adding the sensors into the RECEIVERS after creating a regular grid of receivers. This step is optional.

.. code-block:: groovy
    new Merged_Sensors_Receivers().exec(connection,[
                    "tableReceivers": "RECEIVERS",
                    "tableSensors" : "SENSORS_LOCATION"
    ])

Step 9 : Generate Dynamic Road
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Generate the road ``LW_ROADS_best`` by adjusting dynamically the traffic using <b>BEST_CONFIG</b> according to road type.

.. code-block:: groovy
    new NMs_4_BestConfigs().exec(connection)

Step 10 : Generate the Map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute the attenuation noise level from the network sources emission (LW_ROADS_best) to the receivers.

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

Step 11 : Creation of the result table
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create the map result. The output table is called here ``ASSIMILATED_MAPS`` and contains the noise level at each receiver for the whole time steps.

.. code-block:: groovy
    new Create_Assimilated_Maps().exec(connection,[
                    "bestConfigTable" : "BEST_CONFIGURATION_FULL",
                    "receiverLevel" : "RECEIVERS_LEVEL",
                    "outputTable": "ASSIMILATED_MAPS"
    ])

This table <b>ASSIMILATED_MAPS</b> can be exported as a shape file and imported into qgis to analyze results.

.. code-block:: groovy
    new Export_Table().exec(connection,
                    ["exportPath": workingFolder+"results/ASSIMILATED_MAPS.shp",
                     "tableToExport": "ASSIMILATED_MAPS"
    ])
