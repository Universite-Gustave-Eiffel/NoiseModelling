All_Possible_Configuration
==========================

Generate all possible configurations.

Overview
--------

``All_Possible_Configuration.groovy`` generates all possible combinations of traffic variation values and temperature values.

The combinations are written into the SQL table ``ALL_CONFIGURATIONS``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``trafficValues``
   List of traffic variation values in percent.

   Example format: ``0.01,1.0,2.0,3,4``

   Type: ``String``

``temperatureValues``
   List of temperature values used for road traffic emission.

   Type: ``String``

Output
------

``result``
   Name of the created SQL table: ``ALL_CONFIGURATIONS``.

   Type: ``String``

Function Signatures
-------------------

The script exposes two functions:

* ``exec(Connection connection, input)``
* ``getAllConfig(Connection connection, double[] vals, double[] temps)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It parses the two comma-separated input lists into numeric arrays.
* It creates ``ALL_CONFIGURATIONS`` with the fields ``IT``, ``PRIMARY_VAL``, ``SECONDARY_VAL``, ``TERTIARY_VAL``, ``OTHERS_VAL``, and ``TEMP_VAL``.
* It generates all combinations of the traffic multipliers and temperatures.
* It skips combinations considered incoherent by ratio checks between road classes.

