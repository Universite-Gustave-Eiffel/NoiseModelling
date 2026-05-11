Import_Asc_Folder
=================

Import all ``.asc`` files from a folder.

Overview
--------

``Import_Asc_Folder.groovy`` imports all files with the ``.asc`` extension from a folder into the database.

The script description states that resulting tables should follow the input filenames.

.. figure:: import_asc_folder.png
   :align: center
   :alt: Import asc folder

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFolder``
   Path of the folder to scan.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   Projection identifier of the input ``.asc`` files.

   Default: ``4326``

   Type: ``Integer``

``downscale``
   Number of pixels skipped on each axis.

   Default: ``1``

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

* It recursively scans the folder for ``.asc`` files.
* It tries to read a matching ``.prj`` file per raster, otherwise it falls back to the default or provided SRID.
* It uses the ASCII reader in 3D-point mode and can downscale the raster resolution.

