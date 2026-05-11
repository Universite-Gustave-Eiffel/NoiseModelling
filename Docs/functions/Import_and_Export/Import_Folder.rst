Import_Folder
=============

Import all files with a chosen extension from a folder.

Overview
--------

``Import_Folder.groovy`` imports all files with a specified extension from a folder into the database.

The resulting tables are named after the corresponding input files.

.. figure:: import_folder.png
   :align: center
   :alt: Import folder

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFolder``
   Path of the folder to scan.

   Type: ``String``

``importExt``
   Extension to import, for example ``shp``.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   Original projection identifier of imported geometry tables when needed.

   Default: ``4326``

   Type: ``Integer``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It recursively scans the folder for files whose extension matches ``importExt``.
* It imports each matching file with the corresponding driver and drops any existing destination table first.
* For geometric tables, it creates a spatial index and assigns or validates the SRID.
* If a ``PK`` column exists and no primary key is defined, it promotes ``PK`` to a primary key.

