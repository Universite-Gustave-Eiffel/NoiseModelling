WPS Blocks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


WPS general presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The OGC Web Processing Service (WPS) Interface Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services, such as polygon overlay, as a web service. The WPS standard defines how a client can request the execution of a process, and how the output from the process is handled. It defines an interface that facilitates the publishing of geospatial processes and clients’ discovery of and binding to those processes.


NoiseModelling and WPS
~~~~~~~~~~~~~~~~~~~~~~~~~~~
NoiseModelling v3.0.0 comes with 7 WPS blocks. With each new version, new blocks are added. Be curious and check out the latest version !
WPS scripts for GeoServer are written in groovy language. They are located in the Geoserver\\data_dir\\scripts\\wps directory.

WPS Builder
~~~~~~~~~~~~~~~~~~~~~~~~~~~
WPS Builder allows for the creating of graphical process workflows that can be easily executed and reproduced. It allows Web Processing Services to operate through a user interface.

We have developed a version of WPS Builder adapted to the needs of NoiseModelling. This version being very close to the original version, do not hesitate to consult the official documentation : `WPS Builder documentation`_

.. _WPS Builder documentation: https://docs.boundlessgeo.com/suite/1.1.0/processing/wpsbuilder/index.html

Create your own WPS script
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please see `Advanced Users Section`_, because now you want to be one !

.. _Advanced Users Section : For-Advanced-Users
