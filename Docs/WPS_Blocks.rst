WPS Blocks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


WPS general presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The OGC Web Processing Service (WPS) Interface Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services, such as polygon overlay, as a web service.

The WPS standard defines how a client can request the execution of a process, and how the output from the process is handled. It defines an interface that facilitates the publishing of geospatial processes and clients’ discovery of and binding to those processes.


NoiseModelling and WPS
~~~~~~~~~~~~~~~~~~~~~~~~~~~
NoiseModelling v3.0.0 comes with 7 WPS blocks. WPS scripts for GeoServer are written in groovy language.

They are located in :literal:`Geoserver\\data_dir\\scripts\\wps directory`

.. tip::
    To know the functionality of each WPS block, wait a few moments with your mouse on the block, a tooltip text will appear.

.. note::
    With each new version, new blocks are added. Be curious and check out the latest version !




Create your own WPS block
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please see `Advanced Users Section`_, because now you want to be one !

.. _Advanced Users Section : Own_Wps
