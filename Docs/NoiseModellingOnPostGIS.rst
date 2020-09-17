Use NoiseModelling with a PostGIS database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling is distributed with the GeoServer (http://geoserver.org/) application. This application has been
preconfigured to use H2GIS as the default database.

H2GIS does not need to be configured or installed on the system and is therefore perfectly suitable as a default database.

However, this database is less efficient than the Postgre/PostGIS database, which has a larger community of contributors/users.

NoiseModelling is written with the idea of maintaining H2GIS/PostGIS compatibility.

This tutorial will not cover the steps for installing and configuring a PostGIS database.


Connect with Java
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First you have to add some libraries. We will use PostgreSQL/PostGIS wrapper available in the H2GIS library:


.. literalinclude:: scripts/postgis_deps.xml
   :language: xml
   :linenos:

The new dependency here is postgis-jts-osgi. It contains some code to convert PostGIS geometries objects into/from JTS objects.

In your code you have to import the PostGIS wrapper class and some utility class:

.. literalinclude:: scripts/postgis_nm.java
   :language: java
   :lines: 6-7,19-26
   :linenos:

Then use it to connect to you local or remote PostGIS database and obtain a valid JDBC connection object:

.. literalinclude:: scripts/postgis_nm.java
   :language: java
   :lines: 36-45
   :linenos:

Finally you can use the NoiseModelling functions as usual:

.. literalinclude:: scripts/postgis_nm.java
   :language: java
   :linenos: