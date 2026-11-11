.. DO NOT UPDATE THIS FILE!!
.. This document has been automatically generated with noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/autodoc/GenerateFunctionsDocs.java

Explain Rays
============

Explain propagation rays

Overview
--------

➡️ Display propagation rays of a RAYS table as an interactive HTML fragment.  The block selects the top N rays by LEQ for one receiver, meteo condition and period, then adds the direct ray when it is not already in that selection and when its level is within 20 dB of the total level (meaning it contributes to the receiver level). It draws a 2D scene, a vertical cut profile and a table of attenuation terms.  📋 Prerequisite: run Noise_level_from_source with confRaysName=RAYS so the RAYS table contains IDRECEIVER, IDSOURCE, LEQ, METEO, PERIOD and PATH.  🗺 Optional tables (DEM, BUILDINGS, GROUNDS, RECEIVERS, SOURCES) enrich the scene but are never required. Terrain and ground coefficients are read from PATH first.  📋 Example: tableRays=RAYS, meteo=homogeneous, period=D, maxRays=50. When the direct ray is outside the top N it is appended at the end, so the total can reach N+1 rays.  ℹ️ This block intentionally performs no comparison with observation data, no error computation and has no 3D view.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableRays`` — *RAYS table*
   Table produced with Noise_level_from_source and confRaysName=RAYS. Required columns: IDRECEIVER, IDSOURCE, LEQ, METEO, PERIOD, PATH. Example: RAYS.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID`` — *Source SRID (optional)*
   EPSG code of the coordinates stored in PATH and in the optional tables (for example 2154 for Lambert-93). It is used to transform the 2D scene to WGS84 and to overlay the same OSM-WMS basemap as Table_Visualization_Map. If omitted, the SRID is read from the table geometry; when no SRID is available the basemap is disabled.

   Type: ``Integer``

``maxRays`` — *Maximum rays (optional)*
   Number of rays kept by descending LEQ before adding the direct ray. Default 50, bounded between 1 and 50.

   Type: ``Integer``

   Default: ``50``

``meteo`` — *Meteo (optional)*
   Meteorological condition filter (for example homogeneous or favorable). If omitted, the first distinct value found in the table is used and displayed.

   Type: ``String``

``period`` — *Period (optional)*
   Period filter (for example D, E or N). If omitted, the first distinct value found in the table is used and displayed.

   Type: ``String``

``receiver`` — *Receiver identifier (optional)*
   Receiver to display. If omitted, the receiver with the highest sum of LEQ energy is selected.

   Type: ``Integer``

``tableBuilding`` — *Buildings table (optional)*
   Optional building footprint table. Footprints intersecting the ray bounding box are drawn in the 2D view.

   Type: ``String``

``tableDEM`` — *DEM table (optional)*
   Optional terrain table. The altitudes stored in PATH are used first; this table is accepted for scene enrichment but is not required.

   Type: ``String``

``tableGrounds`` — *Grounds table (optional)*
   Optional ground coefficient table. Coefficients stored in PATH are used first.

   Type: ``String``

``tableReceivers`` — *Receivers table (optional)*
   Optional receiver point table used to place the receiver marker. Without it, the last point of the ray is used.

   Type: ``String``

``tableSources`` — *Sources table (optional)*
   Optional source point table used to place the source marker. Without it, the first point of the ray is used.

   Type: ``String``

Output
------

``result`` — *Result output string*
   HTML fragment of the propagation ray viewer. This output cannot be linked to another block.

   Type: ``String``

