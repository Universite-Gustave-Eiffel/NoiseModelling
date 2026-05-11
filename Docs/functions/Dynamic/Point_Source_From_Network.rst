Point_Source_From_Network
=========================

Create point sources from a network.

Overview
--------

``Point_Source_From_Network.groovy`` creates a ``SOURCES_GEOM`` point-source table from a network linestring table.

This output is useful for computing the attenuation matrix between sources and receivers.

The script creates point sources along the network at a fixed spacing.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableNetwork``
   Name of the input network table.

   It must contain at least:

   * ``PK``: identifier with a primary-key constraint
   * ``THE_GEOM``: geometry column

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``gridStep``
   Distance in meters between possible source locations along the network.

   Default: ``10``

   Type: ``Integer``

``height``
   Source height in meters.

   Default: ``0.05``

   Type: ``Double``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It densifies the input network geometry with the requested spacing and converts the result into multipoints.
* It explodes those multipoints into the ``SOURCES_GEOM`` table and adds an auto-increment primary key.
* It creates a spatial index on ``SOURCES_GEOM(THE_GEOM)``.
* It updates the geometry metadata so the resulting points inherit the network SRID and are forced to 3D with the requested source height.

