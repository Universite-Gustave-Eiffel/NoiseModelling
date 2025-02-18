Dynamic Maps
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Many publications have emerged showcasing the use of **NoiseModelling** to create dynamic maps (see `scientific production`_).

.. _scientific production : https://noisemodelling.readthedocs.io/en/latest/Scientific_production.html

If you'd like to achieve similar results but you feel a bit lost, this tutorial is here to help you navigate through the process.

There are three main approaches to creating dynamic maps using NoiseModelling:

1. **A road network with a single traffic flow**  
   You have a road network and a single traffic flow associated with a specific time period (e.g., 24h). You want to compute dynamic indicators such as **L10**, **L90**, or the **number of events exceeding a threshold** or to get time series for the same time period.

2. **A road network with traffic flows at regular intervals**  
   You have a road network and traffic flow data available at regular intervals (e.g., hourly or every 15 minutes), and you want to generate a dynamic noise map every 15 min.

3. **A road network with associated spatio-temporal data of moving sources**
   You have spatio-temporal information about vehicles moving around a network (e.g., from traffic simulations such as Simuvya or SUMO; or from trajectories of drones). You want to compute **time-series data at each receiver** corresponding to the passage of these sources.

A Word of Caution
-----------------

**Caution** : In all these cases, we assume that the **sound attenuation between the source and receiver remains constant throughout the calculation**. This is a strong approximation, and there are ways to account for variations, but this tutorial will not cover such specific cases.

Dynamic mapping has its subtleties, and it's important to be aware of them to avoid errors. We recommend referring to the following documents for a better understanding of these concepts:

- Can, A., & Aumond, P. (2018). Estimation of road traffic noise emissions: The influence of speed and acceleration. Transportation Research Part D: Transport and Environment, 58, 155-171.
- Gozalo, G. R., Aumond, P., & Can, A. (2020). Variability in sound power levels: Implications for static and dynamic traffic models. Transportation Research Part D: Transport and Environment, 84, 102339.
- Le Bescond, V., Can, A., Aumond, P., & Gastineau, P. (2021). Open-source modeling chain for the dynamic assessment of road traffic noise exposure. Transportation Research Part D: Transport and Environment, 94, 102793.

Assumptions are freely made, specific formats are expected, and so on. To understand the required data formats and check the expected structure of the input tables, please refer also to the example input tables and spatial layers!

Study Cases
---------------

Case 1: A Road Network with a Single Traffic Flow
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Import the road network (with arbitrary traffic flows) and buildings from an OSM file
------------------------------------------------------------------------------------------------

Use Import_OSM WPS block

#. ``Path of the osm file``: Enter the path of the provided Open Street map file (can be relative to NoiseModelling): ``data_dir/data/wpsdata/map.osm.gz``
#. ``Target projection identifier``: Enter the official France projection for this tutorial files ``2154``
#. ``Remove tunnels``: Check it
#. ``Do not import surface``: Check it as we will not use this output

.. figure:: images/tutorial/dynamic/ImportOSM.png
   :align: center

Create a receiver grid using 25 meters step in a grid pattern
------------------------------------------------------------------------------------------------

Use Regular_Grid WPS block

#. ``Buildings``: Enter ``BUILDINGS``
#. ``Sources``: Enter ``ROADS``
#. ``Offset``: Enter ``25`` for 25 meters distance
#. ``Output triangle table``: Check it in order to be able to generate the iso contours

.. figure:: images/tutorial/dynamic/RegularGrid.png
   :align: center


Set all receivers at 1.5 meters height
------------------------------------------------------------------------------------------------

Use Set_Height WPS block

#. ``Table``: Enter ``RECEIVERS``
#. ``Height``: Enter ``1.5``

Convert traffic to dynamic traffic flow
------------------------------------------------------------------------------------------------

From the network with traffic flow to individual trajectories with associated Lw

1. The Probabilistic method, this method place randomly the vehicles on the network according to the traffic flow
1. The Poisson method place the vehicles on the network according to the traffic flow following a poisson law,
 it keeps a coherence in the time series of the noise level

Use the ``Dynamic:Flow_2_Noisy_Vehicles`` WPS block

#. ``Method``: Enter ``PROBA``
#. ``Roads table name``: Enter ``ROADS``
#. ``timestep``: Enter ``1`` (default)
#. ``duration``: Enter ``60`` (default)
#. ``gridStep``: Enter ``10`` (default)

Compute the attenuation for each receiver-source points
------------------------------------------------------------------------------------------------

Use the ``NoiseModelling:Noise_level_from_source`` WPS block

#. ``Buildings table name``: Enter ``BUILDINGS``
#. ``Source geometry table name``: Enter ``SOURCES_GEOM``
#. ``Receivers table name``: Enter ``RECEIVERS``
#. ``Maximum source receiver distance``: Enter ``150``
#. ``Diffraction on horizontal edges``: Check it
#. ``Order of reflexion``: Enter ``0``
#. ``Separate receiver level by source identifier``: Check it to have the SOURCEID column on the output

Apply the source noise level to the attenuation table
------------------------------------------------------------------------------------------------

Compute the noise level from the moving vehicles to the receivers
the output table is called here LT_GEOM and contains the time series of the noise level at each receiver

Use the ``Dynamic:Noise_From_Attenuation_Matrix`` wps block

#. ``Attenuation Matrix Table name``: Enter ``RECEIVERS_LEVEL``
#. ``LW(t)``: Enter ``SOURCES_EMISSION``
#. ``outputTable``: Enter ``LT_GEOM``

Compute noise indicators
------------------------------------------------------------------------------------------------

This step is optional, it compute the LA10, LA50 and LA90 at each receiver from the table LT_GEOM

Use the ``Acoustic_Tools:DynamicIndicators`` wps block

#. ``tableName``: Enter ``LT_GEOM``
#. ``columnName``: Enter ``LAEQ``


Compute iso-surfaces for each time period
------------------------------------------------------------------------------------------------

Generate a dynamic iso-contour map for each time period based on the LAEQ of the receivers.

Use the ``Acoustic_Tools:Create_Isosurface`` wps block

#. ``Sound levels table``: Enter ``LT_GEOM``

``datetime_from_epoch(to_real("PERIOD")*1000+1739869220000)``


Case 2: A Road Network with Traffic Flows at Regular Intervals
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This case is similar to the **MATSim** use case (`here <Matsim_Tutorial.rst>`_), but this tutorial generalizes the approach to fit other datasets.

.. code-block:: groovy

         // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_BUILDINGS.geojson").getPath() ,
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_TRAFFIC.geojson").getPath() ,
                 "inputSRID": "2154",
                 "tableName": "ROADS"])

        // Create a receiver grid
        new Regular_Grid().exec(connection,  ["buildingTableName": "BUILDINGS",
                                              "sourcesTableName" : "ROADS",
                                              "delta"            : 25])

        // Set a height to the receivers at 1.5 m
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                  "height": 1.5
                ])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS",
                "Mode" : "dynamic"])


        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_0DB",
                 "tableReceivers": "RECEIVERS",
                 "maxError" : 0.0,
                 "confMaxSrcDist" : 150,
                 "confDiffHorizontal" : false,
                 "confExportSourceId": true,
                 "confSkipLday":true,
                 "confSkipLevening":true,
                 "confSkipLnight":true,
                 "confSkipLden":true
                ])

        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the noise level at each receiver for the whole timesteps
        new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "LW_ROADS",
                 "lwTable_sourceId" : "LINK_ID",
                 "attenuationTable"   : "LDAY_GEOM",
                 "outputTable"   : "LT_GEOM"
                ])

The toy dataset used in this example was kindly provided by Valentin Lebescond from Université Gustave Eiffel.

Case 3: Spatio-Temporal Data of Moving Sources
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: groovy

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/buildings_nm_ready_pop_heights.shp").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        // Import the receivers (or generate your set of receivers using Regular_Grid script for example)
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/receivers_python_method0_50m_pop.shp").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "receivers"])

        // Set the height of the receivers
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                  "height": 1.5
                ])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/network_tartu_32635_.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "network_tartu"])

        // (optional) Add a primary key to the road network
        new Add_Primary_Key().exec(connection,
                ["pkName" :"PK",
                 "tableName": "network_tartu"])

        // Import the vehicles trajectories
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/SUMO.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "vehicle"])

        // Create point sources from the network every 10 meters. This point source will be used to compute the noise attenuation level from them to each receiver.
        // The created table will be named SOURCES_0DB
        new Point_Source_0dB_From_Network().exec(connection,
                ["tableRoads": "network_tartu",
                 "gridStep" : 10
                ])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_0DB",
                 "tableReceivers": "RECEIVERS",
                 "maxError" : 0.0,
                 "confMaxSrcDist" : 150,
                 "confDiffHorizontal" : false,
                 "confExportSourceId": true,
                 "confSkipLday":true,
                 "confSkipLevening":true,
                 "confSkipLnight":true,
                 "confSkipLden":true
                ])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new Ind_Vehicles_2_Noisy_Vehicles().exec(connection,
                ["tableVehicles": "vehicle",
                "distance2snap" : 30,
                "fileFormat" : "SUMO"])

        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the time series of the noise level at each receiver
        new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "LW_DYNAMIC_GEOM",
                 "attenuationTable"   : "LDAY_GEOM",
                 "outputTable"   : "LT_GEOM"
                ])

        // this step is optional, it compute the LEQA, LEQ, L10, L50 and L90 at each receiver from the table LT_GEOM
        new DynamicIndicators().exec(connection,
                ["tableName"   : "LT_GEOM",
                 "columnName"   : "LEQA"
                ])

The toy dataset was kindly provide by Sacha Baclet from KTH (0000-0003-2114-8680).