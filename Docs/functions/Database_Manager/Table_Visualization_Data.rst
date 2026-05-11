Table_Visualization_Data
========================

Display the first rows of a query result.

Overview
--------

``Table_Visualization_Data.groovy`` displays the content of a SQL query result.

You can provide either a table name or a complete ``SELECT`` SQL query.

The number of displayed rows can be limited with the ``linesNumber`` parameter unless the query already includes a ``LIMIT`` clause.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``tableName``
   Table name or full ``SELECT`` SQL query.

   Examples: ``mytable`` or ``SELECT * FROM mytable``

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``linesNumber``
   Number of rows to display.

   This parameter is ignored if the SQL query already contains a ``LIMIT`` clause.

   Default: ``10``

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes two functions:

* ``exec(Connection connection, input)``
* ``mapToTable(List<Map> list, Sql sql, String queryOrTableName, Connection connection, boolean isTableName)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* If the input is only a table name, the script builds a ``SELECT *`` query automatically.
* For full SQL queries, it allows only ``SELECT``-style behavior and rejects common write operations such as ``DROP``, ``DELETE``, ``UPDATE``, ``INSERT``, ``ALTER``, ``CREATE``, and ``TRUNCATE``.
* If no ``LIMIT`` is present, it appends one using the requested number of rows.
* For table-name input, it also reports row count, SRID information, and primary-key information when available.
* Geometry values are converted to WKT for display in the generated HTML table.

