Create your own WPS block
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The OGC Web Processing Service (`WPS`_) Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services as a web service.

.. _WPS : https://www.ogc.org/standards/wps

WPS scripts for NoiseModelling are written in Groovy language. They are located in the ``NoiseModelling/data_dir/scripts/wps`` directory.

To help you build your WPS script, you will find a template in the ``NoiseModelling/data_dir/scripts/template`` directory

.. note::
    Don't be shy, if you think your script can be useful to the community, you can redistribute it using github or by sending it directly to us.

.. tip::
    The best way to make your own WPS is to be inspired by those that are already made. See how the tutorial is built or contact us for many more examples.

General Structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

1. Import used libraries
-------------------------

::

    import geoserver.GeoServer
    import geoserver.catalog.Store

2. WPS Script meta data
-------------------------

::

    title = '....'
    description = '.....'

3. WPS Script input & output
-----------------------------------

::

    inputs = [
        inputparameter1: [name: '...', description : '...', title: '...', type: String.class],
        inputparameter2: [name: '...', description : '...', title: '...', type: String.class]
    ]

    outputs = [
        ouputparameter: [name: '...', title: '...', type: String.class]
    ]

4. Set connection method
-----------------------------------

::

    def static Connection openPostgreSQLDataStoreConnection() {
        Store store = new GeoServer().catalog.getStore("h2gisdb")
        JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
        return jdbcDataStore.getDataSource().getConnection()
    }

5. Set main method to execute 
-----------------------------------

::

    def run(input) {
    
        // Open connection and close it at the end
        openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->
            // Execute code here
            // for example, run SQL command lines
            Sql sql = new Sql(connection)
            sql.execute("drop table if exists TABLETODROP")    
        }
        
        // print to Console windows
        return [result : 'Ok ! ']
    }
