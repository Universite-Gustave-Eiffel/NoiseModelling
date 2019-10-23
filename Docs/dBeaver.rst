Manipulate your database with dBeaver
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

DBeaver is free universal SQL client/database tool for developers and database administrators. DBeaver is able to connect to H2GIS database which is the one used.

Download dBeaver
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can download dBeaver on the `webpage`_

.. _webpage: https://dbeaver.io/download/

Connect dBeaver to your database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note::
    Be sure that the geoserver is closed. It is not possible to connect dBeaver and GeoServer at the same time.

1. Run dBeaver
2. Add a new connection
3. If you use a h2gis type databse please select 'H2GIS embedded'
4. Browse your database. By default it is in Geoserver\\data_dir and the name is *h2gisdb.mv.db*
5. Open it !

Use dBeaver 
~~~~~~~~~~~~~

Now you can use the full potential of dBeaver and the h2gis database. You can explore, display and manage your database.

Many spatial processing are possible with H2GIS. Please see the `H2GIS website`_.

.. _H2GIS website: http://www.h2gis.org/



Connect dBeaver to NoiseModelling libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You have to load the NoiseModelling library using the grab annotation at the beginning of its script as `here`_.

.. _here: https://github.com/orbisgis/geoclimate


