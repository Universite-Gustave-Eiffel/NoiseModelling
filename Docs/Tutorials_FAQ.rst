Tutorials - FAQ
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Shapefiles ? GeoJSON ?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Shapefile** is a file format for geographic information systems (GIS).

Its extension is classically SHP, and it is always accompanied by two other files with the same name and extensions :

* DBF, a file that contains attribute data relating to the objects contained in the shapefile,
* SHX, file that stores the index of the geometry.

Other files can also be provided :
* .prj - coordinate system information, using the WKT (Well Known Text) format;
* .sbn and .sbx - spatial shape index ;
* .fbn and .fbx - spatial shape index for read-only shapefiles;
* .ain and .aih - attribute index for active fields in a table or in a theme attribute table;
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
