Import_Symuvia
==============

Import Symuvia output data from XML.

Overview
--------

``Import_Symuvia.groovy`` imports Symuvia XML output files into the database.

It creates a trajectory table derived from the XML content.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``pathFile``
   Path of the input ``.xml`` file.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``inputSRID``
   SRID of the Symuvia output geometry.

   Default: ``2154``

   Type: ``Integer``

``tableName``
   Base name of the output table.

   By default, the file name without extension is used.

   Type: ``String``

Output
------

``result``
   Result output string. This output type does not allow blocks to be linked together.

   Type: ``String``

Function Signatures
-------------------

The script exposes one main entry point:

* ``exec(Connection connection, input)``

Execution Notes
---------------

The script comments and inline behavior show the following:

* It imports the XML through a dedicated ``SYMUVIADriverFunction`` parser.
* It creates a ``<tableName>_TRAJ`` table from the imported XML content, including point geometry, speed, acceleration, and time.
* It creates a spatial index on the trajectory table and assigns or validates the SRID.
* If needed, it adds a primary key after import.

