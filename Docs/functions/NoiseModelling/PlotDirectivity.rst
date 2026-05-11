PlotDirectivity
===============

Plot the directivity graph of the specified ``DIR_ID``.

Overview
--------

``PlotDirectivity.groovy`` generates the directivity graph for a selected directivity identifier.

If no directivity table is provided, the script falls back to CNOSSOS-EU train directivity definitions.

The result output is an SVG or HTML fragment containing top, side, and front views of the directivity chart.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``confDirId``
   Identifier of the directivity sphere from ``tableSourceDirectivity``, or a built-in train directivity if no table is provided.

   For train directivity, the documented values are:

   * ``0``: ``OMNIDIRECTIONAL``
   * ``1``: ``ROLLING``
   * ``2``: ``TRACTIONA``
   * ``3``: ``TRACTIONB``
   * ``4``: ``AERODYNAMICA``
   * ``5``: ``AERODYNAMICB``
   * ``6``: ``BRIDGE``

   Type: ``Integer``

``confFrequency``
   Frequency to plot.

   Expected values are ``63``, ``125``, ``250``, ``500``, ``1000``, ``2000``, ``4000``, or ``8000``.

   Type: ``Integer``

Optional inputs
~~~~~~~~~~~~~~~

``tableSourceDirectivity``
   Source directivity table name.

   If not specified, the default is CNOSSOS-EU train directivity.

   The table must contain:

   * ``DIR_ID``: directivity sphere identifier
   * ``THETA``: vertical angle from ``-90`` to ``90`` degrees
   * ``PHI``: horizontal angle from ``0`` to ``360`` degrees
   * ``LW63``, ``LW125``, ``LW250``, ``LW500``, ``LW1000``, ``LW2000``, ``LW4000``, ``LW8000``: attenuation in dB for each octave or third-octave band

   Type: ``String``

``confScaleMinimum``
   Minimum attenuation shown on the scale, in dB.

   Default: ``-35``

   Type: ``Double``

``confScaleMaximum``
   Maximum attenuation shown on the scale, in dB.

   Default: ``0``

   Type: ``Double``

Output
------

``result``
   SVG or HTML representation of the directivity chart.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, Map input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* If ``tableSourceDirectivity`` is supplied, directivity data is loaded from that table.
* Otherwise, the script builds an in-memory set of train directivity definitions.
* The result combines three polar graph orientations: top, side, and front.

