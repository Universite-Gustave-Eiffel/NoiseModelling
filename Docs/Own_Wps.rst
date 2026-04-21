Create your own WPS block
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Presentation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The OGC Web Processing Service (`WPS`_) Standard provides rules for standardizing inputs and outputs (requests and responses) for invoking geospatial processing services as a web service.

.. _WPS : https://www.ogc.org/standards/wps

WPS scripts for NoiseModelling are written in Groovy language. They are located in the ``NoiseModelling/scripts/`` directory.

.. note::
    Don't be shy, if you think your script can be useful to the community, you can redistribute it using github or by sending it directly to us.

.. tip::
    The best way to make your own WPS is to be inspired by those that are already made. See how the tutorial is built or contact us for many more examples.

General Structure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

1. Import used libraries
-------------------------

.. code-block:: groovy

    import groovy.sql.Sql
    import java.sql.Connection
    import org.h2gis.api.ProgressVisitor

2. WPS Script meta data
-------------------------

.. code-block:: groovy

    title = 'My script title'
    description = 'My script description, I support <b>html</b> !'
    inputs = [
        my_optional_parameter: [name: 'option1', title: 'option1', description : 'Option description, you can use html here', type: String.class, min: 0, max: 1],
        my_numeric_parameter: [name: 'my_numeric_option', title: 'my_numeric_option', description : 'Option description, you can use html here', type: Integer.class],
        my_boolean_parameter: [name: 'my_boolean_option', title: 'my_boolean_option', description : 'A checkbox parameter', type: Boolean.class],
        my_choice_parameter: [name: 'my_choice_parameter', title: 'my_choice_parameter', description : 'A list box with limited choices', type: String.class, allowedValues : ["Choice 1", "Choice 2", "Choice 3"], default : "Choice 2"],
    ]

    // Optional (default 60 seconds)
    // For synchronous WPS, it will wait this time (in seconds) before returning a message, but it will still run the execution in the background
    executionTimeout = 120


    outputs = [
        result: [name: 'result', title: 'result', description : 'Result output, generally the result output table name', type: String.class]
    ]

4. Set main method to execute
-----------------------------------

.. code-block:: groovy

    /**
     * Main method
     * @param connection SQL Connection
     * @param input Map of inputs, should provide the same keys as described in the input metadata
     * @param progress Can be used to display the progression of the computation, and to check if the user canceled the execution
     * @return A map as described in the result metadata
     * @throws SQLException if something went wrong
     */
    def exec(Connection connection, Map inputs, ProgressVisitor progress) {
        Sql sql = new Sql(connection)
        def myTableName = inputs.getOrDefault("my_optional_parameter", "MY_RESULT_TABLE")
        sql.execute("CREATE TABLE IF NOT EXISTS $myTableName(PK SERIAL PRIMARY KEY, DATA INTEGER)".toString())
        return [result : '$myTableName']
    }

5. Make your method available

You have to save the script into the folder ``NoiseModelling/scripts/``. You have to refresh the web page WPS Builder in order to see your new script.
