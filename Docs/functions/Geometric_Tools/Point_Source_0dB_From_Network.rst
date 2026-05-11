Point_Source_0dB_From_Network
=============================

Create a 0 dB source table from roads.

Overview
--------

``Point_Source_0dB_From_Network.groovy`` creates a ``SOURCES_0DB`` table from a roads table.

This output can be used with ``Noise_level_from_source`` and ``confExportSourceId=true`` to compute an attenuation matrix independent of absolute source power.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRoads``
   Name of the roads table.

   It must contain at least:

   * ``PK``: primary key identifier
   * ``THE_GEOM``: geometry column

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``gridStep``
   Distance in meters between possible source locations along the network.

   Default: ``10``

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It creates ``SOURCES_0DB`` with geometry and zero-valued octave-band columns.
* It densifies the road geometry using the requested spacing.
* It updates the geometry metadata so the output inherits the road SRID and becomes 3D at ``0.05`` meters height.

