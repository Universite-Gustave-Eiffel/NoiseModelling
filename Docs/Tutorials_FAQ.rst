Tutorials - FAQ
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Shapefiles ? GeoJSON ?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Shapefile** is a file format for geographic information systems (GIS).

Its extension is classically SHP, and it is always accompanied by two other files with the same name and extensions :

* DBF, a file that contains attribute data relating to the objects contained in the shapefile,
* SHX, file that stores the index of the geometry.

Other files can also be provided :

* prj - coordinate system information, using the WKT (Well Known Text) format;
* sbn and sbx - spatial shape index ;
* fbn and fbx - spatial shape index for read-only shapefiles;
* ain and aih - attribute index for active fields in a table or in a theme attribute table;
* etc.

**GeoJSON** (Geographic JSON) is an open format for encoding simple geospatial data sets using the JSON (JavaScript Object Notation) standard.
It is an alternative to the Shapefile format. It has the advantage of being readable directly in a text editor.

PostGreSQL ? H2 ?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
**PostGreSQL** & **H2** are two database management system (DBMS). They are used to store, manipulate or manage, and share information in a database, ensuring the quality, permanence and confidentiality of the information, while hiding the complexity of the operations.
NoiseModelling can connect to DBMS in H2 - H2GIS or PostGreSQL - PostGIS format.

OSM ?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
**OpenStreetMap** (OSM) is a collaborative project to create a free editable map of the world.
The geodata underlying the map is considered the primary output of the project.
The creation and growth of OSM has been motivated by restrictions on use or availability of map data across much of the world, and the advent of inexpensive portable satellite navigation devices.
OSM is considered a prominent example of volunteered geographic information.


Metric SRID ?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Spatial reference systems can be referred to using a **SRID integer**, including EPSG codes.

In several input files, you need to specify coordinates, e.g road network. It is
strongly suggested not to use WGS84 coordinates (i.e. GPS coordinates). Acoustic propagation formulas make the assumption that coordinates are metric.
Many countries and regions have custom coordinate system defined, optimized for usages in their appropriate areas. It might be best to ask some GIS specialists in your region of interest what the most commonly used local coordinate system is and use that as well for your data.
If you don’t have any clue about what coordinate system is used in your region, it might be best to use the Universal Transverse Mercator coordinate system. This coordinate system divides the world into multiple bands, each six degrees width and separated into a northern and southern part, which is called UTM zones (see http://en.wikipedia.org/wiki/UTM_zones#UTM_zone for more details). For each zone, an optimized coordinate system is defined. Choose the UTM zone which covers your region (Wikipedia has a nice map showing the zones) and use its coordinate system.

Here is the map : https://upload.wikimedia.org/wikipedia/commons/e/ed/Utm-zones.jpg

.. note::
  We recommand using the website https://epsg.io/ to find the appropriate **SRID** code for your location.
