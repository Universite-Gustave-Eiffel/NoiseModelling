.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java

PlotDirectivity
===============

Plot the directivity graph of the specified DIR_ID

Overview
--------

➡️ Plot the directivity graph of the specified "DIR_ID"

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``confDirId``
   Identifier of the directivity sphere from "tableSourceDirectivity" parameter or train directivity if "tableSourceDirectivity" parameter is not filled (INTEGER) In case of train, you can use these values:
   
   * 0 = OMNIDIRECTIONAL
   
   * 1 = ROLLING
   
   * 2 = TRACTIONA
   
   * 3 = TRACTIONB
   
   * 4 = AERODYNAMICA
   
   * 5 = AERODYNAMICB
   
   * 6 = BRIDGE

``confFrequency``
   Frequency to plot (INTEGER). 63, 125, 250, 500, 1000, 2000, 4000, 8000 (should match with the column of tableSourceDirectivity

Optional inputs
~~~~~~~~~~~~~~~

``tableSourceDirectivity``
   Name of the emission directivity table.🛠  If not specified the default is train directivity of CNOSSOS-EU  The table must contain the following columns:
   
   *   DIR_ID  : identifier of the directivity sphere (INTEGER)
   
   *   THETA  : [-90;90] Vertical angle in degree. 0° front 90° top -90° bottom (FLOAT)
   
   *   PHI  : [0;360] Horizontal angle in degree. 0° front 90° right (FLOAT)
   
   *   LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000  : attenuation levels in dB for each octave or third octave (FLOAT).

``confScaleMinimum``
   Minimum scale attenuation (in dB)

   Default: ``-35 dB``

``confScaleMaximum``
   Maximum scale attenuation (in dB)

   Default: ``0 dB``

Output
------

``result``
   Svg/Html of the directivity chart

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``
