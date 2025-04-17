Data Assimilation
^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~

In this tutorial, we’ll explore how to model and simulated noise in Geneva using static (road network, buildings) and dynamic (traffic, temperature) data.
The main objective is to combine measurements from sensors located in Geneva with acoustic simulations to identify traffic configurations that best match measurements.

Prerequisites
~~~~~~~~~~~~~~~~~

- You need to have a working installation of NoiseModelling (NM).
- A folder named ``devices_data`` containing sensor measurement files in ``CSV`` format. Each file represents the measurements of a single sensor.
- A ``CSV`` file named ``device_mapping_sf.csv`` containing the geometry and unique identifier of all sensors.
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
The ``device_mapping_sf.csv`` columns ka y are:
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


Step 2 : Preparation of Sensor data
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This part extracts sensor data for a given period.Then use ``trainingRation`` (%) for the training data which will represent the receivers.
So at the end of this part, the ``SENSORS_MEASUREMENTS`` (representing all the data for this period), ``OBSERVATION`` (training data set) and ``RECEIVERS`` sql tables will be created.
And the ``device_mapping_sf.csv`` file is transformed into an sql table named ``SENSORS``.

.. code-block:: groovy
    new Prepare_Sensors().exec(connection,[
                    "startDate":"2024-08-25 06:30:00",
                    "endDate": "2024-08-25 07:30:00",
                    "trainingRatio": 0.8,
                    "workingFolder": workingFolder,
                    "targetSRID": 2056
    ])

Step 3 : Import Buildings and Roads
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

Step 4 : Generate Traffic Emissions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute Road Emission Noise Map from Day Evening Night traffic flow rate and speed estimates.

.. code-block:: groovy
    new Road_Emission_from_Traffic().exec(connection, [
                    "tableRoads": "ROADS"
    ])

Step 5 : Generate an initial Reference Map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute the attenuation noise level from the network sources (LW_ROADS_0DB) to the receivers (at least 500 meters distance).

.. code-block:: groovy
    new Noise_level_from_source().exec(connection, [
                    "tableSources": "LW_ROADS_0DB",
                    "tableBuilding": "BUILDINGS",
                    "tableReceivers": "RECEIVERS",
                    "confExportSourceId": true,
                    "confMaxSrcDist": 500,
                    "confDiffVertical": true,
                    "confDiffHorizontal": true,
                    "confSkipLevening": true,
                    "confSkipLnight": true,
                    "confSkipLden": true
    ])

Step 6 : Generate all Maps
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This step generates all maps by modifying traffic data according to the road type, using data from ``ALL_CONFIGURATIONS``.
The optional ``noiseMapLimit`` parameter limits the number of maps to be generated, in order to avoid ``Out-Of-Memory ``errors.
At the end of this step, the ``NOISE_MAPS`` table containing all the maps is created.

.. code-block:: groovy
    new DataSimulation().exec(connection,[
                    "noiseMapLimit": 80
    ])

Step 7 : Adding the LAEQ
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Add the columns <b>Leq</b> and <b>LAeq</b> to the <b>NOISE_MAPS</b> table with octave band values from 63 Hz to 8000 Hz.

.. code-block:: groovy
    new Add_Laeq_Leq_columns().exec(connection, [
                    "prefix": "HZ",
                    "tableName": "NOISE_MAPS"
    ])

Step 8 : Extract best Configurations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Many maps have been generated, so the best map,the one that minimizes the difference between the measurements and the simulation, must be chosen.
By calculating the difference in LAEQ between simulated (<b>NOISE_MAPS data</b>) and observed (<b>OBSERVATION data</b>) values.
For each time step, the median value of the difference between the two values for all maps is calculated, and the map corresponding to the smallest median value will be the best map.
At the end the ``BEST_CONFIG`` table is created.

.. code-block:: groovy
    new Extract_Best_Configuration().exec(connection,[
                    "observationTable": "OBSERVATION",
                    "noiseMapTable": "NOISE_MAPS"
    ])

Execute Simulation: Generate the Dynamic Map
-----------------
This pars is designed to execute a dynamic traffic calibration process using the best configuration.

Step 9 : Generate new Receivers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create a regular grid of over 4000 receivers.

.. code-block:: groovy
    new Regular_Grid().exec(connection,[
                    "buildingTableName": "BUILDINGS",
                    "sourcesTableName":"ROADS",
                    "delta": 200
    ])

Step 10 : Generate Dynamic Road
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Generate the road by adjusting dynamically the traffic using <b>BEST_CONFIG</b> according to road type.
.. code-block:: groovy
    new Dynamic_Road_Traffic_Emission().exec(connection)

Step 11 : Generate Dynamic Traffic Emissions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Execute road emission and noise level calculations with dynamic mode.

.. code-block:: groovy
    new Road_Emission_from_Traffic().exec(connection, [
                    "tableRoads": "DYNAMIC_ROADS",
                    "mode": "dynamic"
    ])

Step 12 : Generate the Map
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers.

.. code-block:: groovy
    new Noise_level_from_source().exec(connection, [
                    "tableBuilding": "BUILDINGS",
                    "tableSources": "SOURCES_0DB",
                    "tableReceivers": "RECEIVERS",
                    "maxError": 0.0,
                    "confMaxSrcDist": 250,
                    "confDiffHorizontal": false,
                    "confExportSourceId": true,
                    "confSkipLday": true,
                    "confSkipLevening": true,
                    "confSkipLnight": true,
                    "confSkipLden": true
    ])

Step 13 : Generate Dynamic Noise Map From Attenuation Matrix
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute the noise level from the moving vehicles to the receivers.
The output table is called here ``LT_GEOM`` and contains the noise level at each receiver for the whole time steps.

.. code-block:: groovy
    new Noise_From_Attenuation_Matrix().exec(connection, [
                    "lwTable": "LW_ROADS",
                    "lwTable_sourceId": "LINK_ID",
                    "attenuationTable": "LDAY_GEOM",
                    "sources0DBTable": "SOURCES_0DB",
                    "outputTable": "LT_GEOM"
    ])

This table <b>LT_GEOM</b> can be exported as a shape file and imported into qgis to analyze results.
.. code-block:: groovy
    new Export_Table().exec(connection,
                    ["exportPath": workingFolder+"results/LT_GEOM.shp",
                     "tableToExport": "LT_GEOM"
    ])
