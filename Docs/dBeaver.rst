Manipulate your database with DBeaver
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

`DBeaver`_ is a free and open-source universal SQL client/database tool for developers and database administrators. DBeaver is able to connect to `H2GIS`_ database which is the one used.

.. _DBeaver: https://dbeaver.io/
.. _H2GIS: http://www.h2gis.org/

Download DBeaver
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can download DBeaver on this `webpage`_.

.. _webpage: https://dbeaver.io/download/

Connect DBeaver to your database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note::
    Be sure that the Geoserver is closed. It is not possible to connect DBeaver and GeoServer at the same time.

1. Run DBeaver
2. Add a new connection
3. If you use a H2GIS type databse, please select 'H2GIS embedded'
4. Browse your database. By default it is in the ``NoiseModelling\\data_dir`` directory and the name is ``h2gisdb.mv.db``.
5. Open it !

Use DBeaver 
~~~~~~~~~~~~~

Now you can use the full potential of DBeaver and the H2GIS database. You can explore, display and manage your database.

Many spatial processing are possible with H2GIS. Please have a look to the numerous functions on the `H2GIS website`_.

.. _H2GIS website: http://www.h2gis.org/docs/dev/functions/



Connect DBeaver to NoiseModelling libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You have to load the NoiseModelling library using the grab annotation at the beginning of its script as `here`_.

.. _here: https://github.com/orbisgis/geoclimate


