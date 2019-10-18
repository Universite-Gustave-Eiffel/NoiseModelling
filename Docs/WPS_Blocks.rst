WPS Blocks
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

NoiseModelling v3.0 comes with 7 WPS blocks.

.. note ::
    - Orange blocks are mandatory
    - Beige blocks are optional
    - if all input blocks are optional, you must modify at least one of these blocks to be able to run the process
    - Blocks get solid border when they are ready to run


Import and Export 
~~~~~~~~~~~~~~~~~~~~~~~~

- **[Import Folder]** : Allows you to import all files (with the same format) from a folder.

- **[Import File]** : Allows you to import an unique file.

- **[Export File]** : Allows you to export an unique file.

.. note ::
    The supported format are : csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv


Noise Modelling
~~~~~~~~~~~~~~~~~~~~~~~~

- **[Get Lday (Tutorial)]** : Allows you to compute your first noise map with NoiseModelling. This is a tutorial. 

.. note ::
    In this tutorial, you can use 5 different tables as input data containing the following fields: 
        - DEM (dem_lorient.geojson) : 
            - geometry : POINT with 3 dimensions (X,Y,Z) 
        - BUILDING (buildings.shp) : 
            - geometry : POLYGONS
            - HEIGHT : building height.
        - SOURCES (roads.shp) : 
            - geometry : LINES
            - ID : primary key corresponding to the roads ID 
            - AADF field : Annual Average Daily Flow estimates
            - CLAS_ADM : type of roads (used to estimate flow speed)
        - RECEIVERS (receivers.shp) : 
            - geometry : POINT with 3 dimensions (X,Y,Z) 
            - ID : primary key
        - GROUND (ground_type.shp) :
            - geometry : POLYGONS
            - G : from 0 to 1 the absorption coefficient of the ground


Database management
~~~~~~~~~~~~~~~~~~~~~~~~

- **[Display All]**  : Display all tables in your database. You can choose to log the column names or not.

- **[Clean All]**  : Clean all tables in your database.

- **[Drop a table]**  : Remove one specific table of your database.

Create your own WPS script
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please see `Advanced Users Section`_, because now you want to be one !

.. _Advanced Users Section : For-Advanced-Users
