.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

Point Source From Network
=========================

Create Point Source From a network

Overview
--------

Creates a SOURCES_GEOM point source table from a network linestring table. This table is useful to compute the attenuation matrix between sources and receivers.Create point sources from the network every "gridStep" meters. This point source will be used to compute the noise attenuation level from them to each receiver.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableNetwork``
   Name of the network table.
   
   Must contain at least:- PK: identifier with a Primary Key constraint- THE_GEOM: geometric column

Optional inputs
~~~~~~~~~~~~~~~

``gridStep``
   Distance between location of possible sources along the network in

   Default: ``10``

``height``
   Height of the source in

   Default: ``0.05``

Output
------

``result``
   This type of result does not allow the blocks to be linked together.

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
