Blocks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

NoiseModelling provides processing blocks (referred to as **Blocks**) that encapsulate geospatial computations. These blocks are built on top of the OGC Web Processing Service (`WPS`_) standard.


WPS general presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The OGC Web Processing Service (`WPS`_) Interface Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services, such as polygon overlay, as a web service.

The WPS standard defines how a client can request the execution of a process, and how the output from the process is handled. It defines an interface that facilitates the publishing of geospatial processes and clients' discovery of and binding to those processes.

.. _WPS: https://www.ogc.org/standards/wps

NoiseModelling and WPS
~~~~~~~~~~~~~~~~~~~~~~~~~~~
Since release v.3.0.0, NoiseModelling comes with various scripts, encapsulated in so-called Blocks. These Blocks (also referred to as groovy scripts or ``.groovy`` files *(openable in any text editor)*) are written in the `Groovy`_ script language.

Physically stored in the ``NoiseModelling/scripts`` directory.

.. tip::
    To know the functionality of each Block, wait a few moments with your mouse on the Block, a tooltip text will appear.

.. note::
    With each new version, new Blocks are added. Be curious and check out the latest version!

.. _Groovy: https://groovy-lang.org/

Create your own block
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please see :doc:`Own_Blocks`, because now you want to be one!

