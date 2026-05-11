Add_Laeq_Leq_columns
====================

Add ``Leq`` and ``LAeq`` columns.

Overview
--------

``Add_Laeq_Leq_columns.groovy`` adds ``LEQ`` and ``LEQA`` columns to a table containing octave-band values from ``63 Hz`` to ``8000 Hz``.

The script expects frequency columns such as ``HZ63``, ``HZ125``, up to ``HZ8000``, using a configurable prefix.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``prefix``
   Prefix of the octave-band columns.

   For example: ``HZ``

   Type: ``String``

``tableName``
   Name of the table on which ``LEQ`` and ``LEQA`` columns will be added.

   Type: ``String``

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

* The input prefix is normalized to uppercase before column lookup.
* The script checks that the table contains at least the ``<PREFIX>63`` column before proceeding.
* It adds computed ``LEQA`` and ``LEQ`` columns using SQL expressions derived from the octave bands.

