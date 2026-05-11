Merged_Sensors_Receivers
========================

Merge sensors into the receivers table.

Overview
--------

``Merged_Sensors_Receivers.groovy`` adds sensor locations into an existing receivers table after a regular receiver grid has been created.

The result is a single receiver table containing both map receivers and sensors.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableReceivers``
   Receiver table name.

   Type: ``String``

``tableSensors``
   Sensors table name.

   Type: ``String``

Output
------

``result``
   Receiver table containing all sensors.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, inputs)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It drops the ``ID_ROW`` and ``ID_COL`` columns from the receivers table.
* It adds an ``IDNAME`` column and marks existing receivers with ``REC_MAP``.
* It inserts sensor geometries into the receivers table and uses ``IDSENSOR`` as the inserted ``IDNAME``.

