Use NoiseModelling with a PostGIS database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling is distributed with `GeoServer`_. This application has been preconfigured to use `H2GIS`_ as the default database.

H2GIS does not need to be configured or installed on the system and is therefore perfectly suitable as a default database.

However, you may want to connect NoiseModelling to a `PostgreSQL`_/`PostGIS`_ database (this option may be interesting especially if you are using huge datasets (*e.g* on large area)).

That is why NoiseModelling has been written with the idea of maintaining the H2GIS/PostGIS compatibility.

This tutorial will not cover the steps for installing and configuring a PostGIS database.

.. _Geoserver: http://geoserver.org/
.. _H2GIS : http://h2gis.org/
.. _PostgreSQL: https://www.postgresql.org/
.. _PostGIS: https://postgis.net/


Connect with Java
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

First you have to add some libraries. We will use PostgreSQL/PostGIS wrapper available in the H2GIS library:


.. literalinclude:: scripts/postgis_deps.xml
   :language: xml
   :linenos:

The new dependency here is ``postgis-jts-osgi``. It contains some code to convert PostGIS geometries objects into/from `JTS`_ objects.

.. _JTS: https://github.com/locationtech/jts

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