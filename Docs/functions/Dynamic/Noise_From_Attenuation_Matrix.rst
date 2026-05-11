Noise_From_Attenuation_Matrix
=============================

Create a noise map from an attenuation matrix.

Overview
--------

``Noise_From_Attenuation_Matrix.groovy`` combines a source-emission table with an attenuation matrix to build a receiver noise map.

It creates a new output table by summing source emissions and attenuation contributions for each receiver and period.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``lwTable``
   Source-emission table, for example ``SOURCES_EMISSION``.

   The table must contain:

   * ``IDSOURCE``
   * ``PERIOD``
   * ``HZ63`` to ``HZ8000``

   ``IDSOURCE`` links to the attenuation table primary key, and ``PERIOD`` is a text value.

   Type: ``String``

``attenuationTable``
   Attenuation matrix table, typically obtained from ``Noise_level_from_source`` with ``confExportSourceId`` enabled.

   It should typically be ``RECEIVERS_LEVEL`` and must contain:

   * ``IDRECEIVER``
   * ``IDSOURCE``
   * ``THE_GEOM``
   * ``HZ63`` to ``HZ8000``

   Type: ``String``

``outputTable``
   Name of the output table to create.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``lwTable_sourceId``
   Field name used as the source identifier in the LW table.

   Default: ``IDSOURCE``

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

* It joins the attenuation table with the LW table on source identifier.
* For each receiver and period, it performs an energetic sum over all contributing sources for each octave band.
* It adds computed ``LAEQ`` and ``LEQ`` columns to the output table.
* It creates a unique index on ``IDRECEIVER`` and ``PERIOD`` in the output table.

