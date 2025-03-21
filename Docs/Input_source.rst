Sound source
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

NoiseModelling is a tool for producing noise maps. To do so, at different stages of the process, the application needs input data, respecting a strict formalism.

Below we describe the table ``SOURCES_GEOM`` and ``SOURCES_EMISSION``, expected by the ``Noise_level_from_source`` processing block.

The goal is to define any sound source with a custom noise spectrum level.

SOURCES_GEOM Table definition
---------------------

* ``PK`` *
    * Description: An identifier (must be a PRIMARY KEY). The field name is not important, it could be IDSOURCE.
    * Type:  Integer
* ``THE_GEOM`` *
    * Description: Geometry of the source (``LINESTRING`` , ``MULTILINESTRING`` or ``POINT``). The geometry **must** have a Z attribute.
    * Type: Geometry

.. warning::
    * ``Zobject`` = ``Zdem + Zsource``
    * If there is no DEM, the altitude will be equal to geometry Z attribute

.. note::
    For backward compatibility in this table you can define here the noise level for Day Evening and Night, for 500 Hz the expected field is ``HZD500`` ``HZE500`` ``HZN500``. NoiseModelling will output the level for this periods and also compute the global ``DEN`` period levels according to standard.

If you provide only the geometry NoiseModelling will compute only the attenuation on the output table ``RECEIVERS_LEVEL``.

To provide the emission level you must define an emission table for each time period.

SOURCES_EMISSION Table definition
---------------------

* ``IDSOURCE`` *
    * Description: An identifier linked to the primary key of the ``SOURCES_GEOM`` table.
    * Type:  Integer
* ``PERIOD`` *
    * Description: Identifier of the time. ex. `8h00-9h00`
    * Type:  String
* ``HZ63``
    * Description: emission levels in dB for 63 Hz
    * Type: Double
* ``HZ125``
    * Description: emission levels in dB for 125 Hz
    * Type: Double
* ``HZ250``
    * Description: emission levels in dB for 250 Hz
    * Type: Double
* ``HZ500``
    * Description: emission levels in dB for 500 Hz
    * Type: Double
* ``HZ1000``
    * Description: emission levels in dB for 1000 Hz
    * Type: Double
* ``HZ2000``
    * Description: emission levels in dB for 2000 Hz
    * Type: Double
* ``HZ4000``
    * Description: emission levels in dB for 4000 Hz
    * Type: Double
* ``HZ8000``
    * Description: emission levels in dB for 8000 Hz
    * Type: Double

.. note::
    You can define partially the bands. We defined here only octave bands but NoiseModelling is supporting third-octaves ``HZ50, HZ63, HZ80, HZ100, HZ125, HZ160, HZ200, HZ250, HZ315, HZ400, HZ500, HZ630, HZ800, HZ1000, HZ1250, HZ1600, HZ2000, HZ2500, HZ3150, HZ4000, HZ5000, HZ6300, HZ8000, HZ10000``.

