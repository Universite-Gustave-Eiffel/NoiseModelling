.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java

PlotDirectivity
===============

Plots the directivity graph of the specified DIR_ID

Overview
--------

➡️ Plots the directivity graph of the specified "DIR_ID"

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``confDirId`` — *Directivity Index*
   Identifier of the directivity sphere from "tableSourceDirectivity" parameter or train directivity if "tableSourceDirectivity" parameter is not filled (INTEGER) In case of train, you can use these values:
   
   * 0 = OMNIDIRECTIONAL
   
   * 1 = ROLLING
   
   * 2 = TRACTIONA
   
   * 3 = TRACTIONB
   
   * 4 = AERODYNAMICA
   
   * 5 = AERODYNAMICB
   
   * 6 = BRIDGE

   Type: ``Integer``

``confFrequency`` — *Frequency*
   Frequency to plot (INTEGER). 63, 125, 250, 500, 1000, 2000, 4000, 8000 (should match with the column of tableSourceDirectivity

   Type: ``Integer``

Optional inputs
~~~~~~~~~~~~~~~

``confScaleMaximum`` — *Maximum scale attenuation (dB)*
   Maximum scale attenuation (in dB)

   Type: ``Double``

   Default: ``0``

``confScaleMinimum`` — *Minimum scale attenuation (dB)*
   Minimum scale attenuation (in dB)

   Type: ``Double``

   Default: ``-35``

``tableSourceDirectivity`` — *Source directivity table name*
   Name of the emission directivity table.🛠  If not specified the default is train directivity of CNOSSOS-EU  The table must contain the following columns:
   
   *   DIR_ID  : identifier of the directivity sphere (INTEGER)
   
   *   THETA  : [-90;90] Vertical angle in degree. 0° front 90° top -90° bottom (FLOAT)
   
   *   PHI  : [0;360] Horizontal angle in degree. 0° front 90° right (FLOAT)
   
   *   LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000  : attenuation levels in dB for each octave or third octave (FLOAT).

   Type: ``String``

Output
------

``result`` — *Result output string*
   Svg/Html of the directivity chart

   Type: ``String``

