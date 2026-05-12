.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

Road Emission From AADF
=======================

Compute Road Emission

Overview
--------

➡️ Compute Road Emission Noise Map from Estimated Annual average daily flows (AADF) estimates.
This block allows to calculate a road traffic noise emission map from the AADF estimates given in the ROADS.shp file of the tutorial. The average traffic is first converted to hourly traffic before the calculation of Lday, Levening and Lnight using distribution in Berengier et al., 2019 : "DEUFRABASE: A Simple Tool for the Evaluation of the Noise Impact of Pavements in Typical Road Geometries".

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``sourcesTableName`` — *Sources table name*

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``databaseName`` — *Name of the database*
   Name of the database (default : first found db)

   Type: ``String``

Output
------

``result`` — *Result*

   Type: ``String``

