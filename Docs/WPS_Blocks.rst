WPS Blocks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


WPS general presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The OGC Web Processing Service (`WPS`_) Interface Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services, such as polygon overlay, as a web service.

The WPS standard defines how a client can request the execution of a process, and how the output from the process is handled. It defines an interface that facilitates the publishing of geospatial processes and clients’ discovery of and binding to those processes.

.. _WPS: https://www.ogc.org/standards/wps

NoiseModelling and WPS
~~~~~~~~~~~~~~~~~~~~~~~~~~~
Since release v.3.0.0, NoiseModelling comes with various WPS scripts, encapsulated in so-called blocks. These blocks, written in `Groovy`_ language, are executed thanks to the `GeoServer`_ WPS engine.

Physically stored as ``.groovy`` files *(openable in any text editor)*, they are located in the ``NoiseModelling\\data_dir\\scripts\\wps\\`` directory.

.. tip::
    To know the functionality of each WPS block, wait a few moments with your mouse on the block, a tooltip text will appear.

.. note::
    With each new version, new blocks are added. Be curious and check out the latest version!


.. _Geoserver: https://geoserver.org/
.. _Groovy: https://groovy-lang.org/

Create your own WPS block
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please see `Advanced Users Section`_, because now you want to be one!

.. _Advanced Users Section : Own_Wps
