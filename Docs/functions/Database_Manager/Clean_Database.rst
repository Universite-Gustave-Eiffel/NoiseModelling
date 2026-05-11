Clean_Database
==============

Delete all database tables.

Overview
--------

``Clean_Database.groovy`` deletes all non-system tables from the database.

This operation is explicitly marked for cautious use.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``areYouSure``
   Confirmation flag indicating whether all database tables should be deleted.

   Type: ``Boolean``

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

* It ignores the system tables ``SPATIAL_REF_SYS`` and ``GEOMETRY_COLUMNS``.
* If ``areYouSure`` is ``true``, it drops every other table found in the ``PUBLIC`` schema.
* If ``areYouSure`` is ``false``, the script leaves the database unchanged.

