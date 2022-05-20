Manage data on H2 database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Introduction
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling is distributed with `GeoServer`_. This application has been preconfigured to use `H2`_ / `H2GIS`_ as the default database (to store and manage all the needed data).

This database does not need to be configured or installed on the system. It's transparent to users.


To visualize and manage NoiseModelling data (*e.g* roads, buildings or landcover layers) you have the choice between the two following approaches:

#. Use WPS blocks
#. Use H2/H2GIS web client


1. Use WPS blocks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once NoiseModelling UI is launched (open `http://localhost:9580/ <http://localhost:9580/>`_ in your web browser), you can manage your data thanks to the ``Database_Manager`` WPS blocks folder *(on the left side)*. In particular, you can do these actions:

- ``Add_Primary_Key``: allows to add a primary key on a column of a specific layer (table)
- ``Clean_Database``: remove all the layers (tables) from NoiseModelling *(can be useful when starting a new project)*
- ``Display_Database``: list all the layers (tables) and the columns inside
- ``Drop_a_Table``: remove the selected layer (table) from NoiseModelling
- ``Table_Visualization_Data``: display the layer (table) as an array of values  
- ``Table_Visualization_Map``: if the layer (table) is geographic (contains geometry(ies)), display the data in a map 

Below is an illustration with the ``Display_Database`` WPS block

.. figure:: images/NoiseModellingOnH2/nm_db_view.png
    :align: center
    :width: 100%


2. Use H2/H2GIS web client
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you want to have full capabilities on visualization, edition and processing on data, you may need to connect to the db thanks to the H2 web interface.

To do so, once NoiseModelling is started, open your favorite web browser, and copy-paste *(or click on)* this URL `localhost:9580/geoserver/h2/ <localhost:9580/geoserver/h2/>`_

In the connexion panel, you have to specify the following informations:

- ``Driver Class``: the driver that allows to connect to a specific database. Here we want to connect to a H2 db, so let the default value ``org.h2.Driver``
- ``JDBC URL`: the JDBC address of the NoiseModelling database. By default, this db is placed in here ``/.../data_dir/h2gisdb.mv.db``. So, fill this text area with ``jdbc:h2:/.../data_dir/h2gisdb.mv.db``
- ``User name``: the db user name. By default, keep the empty value
- ``Password``: the db password. By default, keep the empty value


Below is an example, with a database located on the computer here: ``/home/nm_user/NoiseModelling/NoiseModelling_3.4.5/data_dir/h2gisdb.mv.db``

- ``JDBC URL``: ``jdbc:h2:/home/nm_user/NoiseModelling/NoiseModelling_3.4.5/data_dir/h2gisdb.mv.db``
- ``User name``: *empty*
- ``Password``: *empty*

.. figure:: images/NoiseModellingOnH2/connexion_panel.png
    :align: center
    :width: 50%

.. warning::
    The URL is here adapted to Linux or Mac users. Windows user may adapt the address by replacing ``/`` by ``\`` and the drive name.

Once done, click on ``Connect``

In the new interface, you discover a full db manager, with the list of tables on the left side, a SQL console (where you can execute all the instructions you want, independently of NoiseModelling) and a result panel. 

.. figure:: images/NoiseModellingOnH2/h2_db_view.png
    :align: center
    :width: 100%


.. note::
    Unlike DBeaver (see ":doc:`dBeaver`"), you can open this interface (and manipulate data) at the same time as NoiseModelling


.. _Geoserver: http://geoserver.org/
.. _H2 : https://www.h2database.com
.. _H2GIS : http://h2gis.org/
.. _PostgreSQL: https://www.postgresql.org/
.. _PostGIS: https://postgis.net/