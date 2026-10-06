Use NoiseModelling with a PostGIS database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling is designed to use `H2GIS`_ as the default database.

H2GIS does not need to be configured or installed on the system and is therefore perfectly suitable as a default database.

However, you may want to connect NoiseModelling to a `PostgreSQL`_/`PostGIS`_ database (this option may be interesting especially if you want to use `QGIS`_ while using NoiseModelling).

.. note:: Using PostGIS may result in slower performance than H2GIS due to network overhead, as data is transferred over a connection rather than written directly to local storage.

NoiseModelling core functions has been written with the idea of maintaining the H2GIS/PostGIS compatibility.

.. warning:: NoiseModelling Groovy scripts may use incompatible syntax with PostGIS. We are currently working on checking the compatibility with the PostGIS database.

This tutorial will not cover the steps for installing and configuring a PostGIS database.

.. _QGIS : https://qgis.org
.. _H2GIS : https://h2gis.org/
.. _PostgreSQL: https://www.postgresql.org/
.. _PostGIS: https://postgis.net/

Using ScriptRunner with PostGIS
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can run Groovy scripts using a PostGIS connection instead of the default H2GIS embedded database by using the `ScriptRunner` program.

This is particularly useful for large-scale simulations where a dedicated database server is preferred.

Command Line Example
--------------------

To import a Shapefile into a PostGIS database, you can use the following command:

.. code-block:: bash

    ./bin/ScriptRunner \
        -w /path/to/working/dir \
        -s src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy \
        -d noisemodelling_db \
        -u noisemodelling \
        -p noisemodelling \
        --host localhost \
        --port 5432 \
        --pathFile /path/to/your/buildings.shp \
        --tableName BUILDINGS

Parameters Description
----------------------

*   **-w, --working-dir**: Path where the application logs and temporary files will be stored.
*   **-s, --script**: Path to the Groovy script you want to execute.
*   **-d, --database-name**: Name of the PostGIS database.
*   **-u, --username**: Database username.
*   **-p, --password**: Database password. If not provided and a host is specified, it will try to fetch it from the `.pgpass` file.
*   **--host**: PostGIS database host name. Specifying this parameter tells NoiseModelling to connect to PostGIS instead of using H2GIS.
*   **--port**: PostGIS database port (default: 5432).

Additional Script Parameters
----------------------------

Any parameter specific to the Groovy script (defined in its `inputs` map) can be passed as a double-dash argument. In the example above:

*   **--pathFile**: The path to the file to import (required by `Import_File.groovy`).
*   **--tableName**: The name of the table to create in the database.

Using .pgpass for Credentials
-----------------------------

If you don't want to provide the password in the command line, you can use a `.pgpass` file as specified in the `PostgreSQL documentation <https://www.postgresql.org/docs/current/libpq-pgpass.html>`_.

The file should contain lines in the following format:

.. code-block:: text

    hostname:port:database:username:password

NoiseModelling will automatically fetch the password if the host is specified and the password parameter is omitted.

