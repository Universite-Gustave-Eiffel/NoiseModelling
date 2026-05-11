Extract_Best_Configuration
==========================

Extract the best configurations.

Overview
--------

``Extract_Best_Configuration.groovy`` extracts the best simulated maps by minimizing the difference between measured and simulated values.

It does this by calculating minimum median absolute differences and creates the table ``BEST_CONFIGURATION_FULL``.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``observationTable``
   Table of sensor observations containing the training dataset.

   Type: ``String``

``noiseMapTable``
   Table containing the simulated noise maps.

   Type: ``String``

``tempToleranceThreshold``
   Temperature tolerance threshold used to extract the best configuration.

   Type: ``Double``

Output
------

``result``
   Created table ``BEST_CONFIGURATION_FULL``.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It adds a ``TEMP`` column to ``RECEIVERS_LEVEL`` and fills it from ``FILTERED_CONFIGURATIONS``.
* It computes median observed temperatures per epoch and average simulated temperatures per period.
* It builds a ``BEST_TEMP`` table matching observation epochs to simulated periods within the configured temperature tolerance.
* For each epoch, it computes the median absolute difference between observed and simulated ``LAEQ`` values and keeps the minimum.
* It joins the selected periods back to ``FILTERED_CONFIGURATIONS`` to create ``BEST_CONFIGURATION_FULL``.

