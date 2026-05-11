Prepare_Sensors
===============

Prepare sensor data and create SQL tables.

Overview
--------

``Prepare_Sensors.groovy`` extracts sensor data for a given period and creates the SQL tables needed for the data-assimilation workflow.

It creates ``SENSORS_MEASUREMENTS``, ``SENSORS_LOCATION``, and ``SENSORS_MEASUREMENTS_TRAINING``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``startDate``
   Start timestamp used to extract the dataset.

   Expected format: ``%Y-%m-%d %H:%M:%S``

   Type: ``String``

``endDate``
   End timestamp used to extract the dataset.

   Expected format: ``%Y-%m-%d %H:%M:%S``

   Type: ``String``

``trainingRatio``
   Training-data percentage of the total data.

   Type: ``Float``

``workingFolder``
   Folder containing the CSV input files, including ``device_mapping_sf``, the OSM file, and the ``devices_data`` folder.

   Type: ``String``

``targetSRID``
   Target projection identifier for the created tables.

   It should be a metric EPSG code.

   Type: ``Integer``

Output
------

``result``
   Created SQL tables ``SENSORS_MEASUREMENTS``, ``SENSORS_LOCATION``, and ``SENSORS_MEASUREMENTS_TRAINING``.

   Type: ``String``

Function Signatures
-------------------

The script exposes four functions:

* ``exec(Connection connection, input)``
* ``allMeasurements(LocalDateTime dayStart, LocalDateTime dayEnd, String deviceFolder)``
* ``measurementTable(Connection connection, List<Map<String, String>> selectedData)``
* ``extractObservationData(Connection connection, Float ratio)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It loads sensor CSV files from the ``devices_data`` folder and filters records between the requested timestamps.
* ``parseTimestamp`` supports millisecond timestamps, ISO timestamps with ``T``, and timestamps with a space separator.
* It builds ``SENSORS_MEASUREMENTS`` from the selected CSV rows and renames ``deveui`` to ``IDSENSOR`` and ``Leq`` to ``LAEQ``.
* It links measurements to ``SENSORS_LOCATION`` geometries and receiver identifiers.
* It creates ``SENSORS_MEASUREMENTS_TRAINING`` by randomly selecting a reproducible subset of sensors based on the requested training ratio.

