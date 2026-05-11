Export_Table
============

Export a database table to a local file.

Overview
--------

``Export_Table.groovy`` exports a table from the database into a local file.

Supported output extensions include ``csv``, ``dbf``, ``geojson``, ``json``, ``kml``, ``shp``, ``tsv``, and ``fgb``.

.. figure:: export_table.png
   :align: center
   :alt: Export table

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``exportPath``
   Output file path, including the extension.

   Example: ``c:/home/receivers.geojson``

   Type: ``String``

``tableToExport``
   Table name or SQL query to export.

   The metadata documents two forms:

   * a simple table name
   * a ``SELECT`` query wrapped in parentheses

   Type: ``String``

Output
------

``result``
   Exported table name that can be reused as an input for another process.

   Type: ``String``

Function Signatures
-------------------

The script exposes two entry points:

* ``exec(Connection connection, Map input, ProgressVisitor progress)``
* ``exec(Connection connection, Map input)``

The second form calls the first one with a ``RootProgressVisitor``.

Execution Notes
---------------

The script comments and inline behavior show the following:

* It checks that the table is not empty before exporting.
* It chooses the export driver from the file extension.
* It reports the SRID of the exported table when available.

