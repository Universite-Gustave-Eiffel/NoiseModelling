Plot_Exposition_Distribution
============================

Plot the distribution of a noise-exposure field.

Overview
--------

``Plot_Exposition_Distribution.groovy`` displays a graph showing the distribution of a selected field in a previously calculated MATSim agent-exposure table.

The script opens a graph window on the server.

Arguments
---------

Mandatory inputs
~~~~~~~~~~~~~~~~

``expositionsTableName``
   Name of the table containing the exposure values.

   The table must contain fields such as:

   * ``PK``
   * ``PERSON_ID``
   * ``HOME_FACILITY``
   * ``HOME_GEOM``
   * ``WORK_FACILITY``
   * ``WORK_GEOM``
   * ``LAEQ``
   * ``HOME_LAEQ``
   * ``DIFF_LAEQ``

   Type: ``String``

``expositionField``
   Field containing the exposure values to plot.

   Type: ``String``

Optional inputs
~~~~~~~~~~~~~~~

``otherExpositionField``
   Second exposure field to plot for comparison.

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

* It computes cumulative percentages of agents exposed above each level from ``20`` to ``100`` dB.
* It can plot one or two exposure series depending on whether the comparison field is provided.
* It renders the result with JFreeChart in a Swing window on the server.

