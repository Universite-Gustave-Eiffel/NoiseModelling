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
* ``LW63``
	* Description: emission levels in dB for 63 Hz
	* Type: Double
* ``LW125``
	* Description: emission levels in dB for 125 Hz
	* Type: Double
* ``LW250``
	* Description: emission levels in dB for 250 Hz
	* Type: Double
* ``LW500``
	* Description: emission levels in dB for 500 Hz
	* Type: Double
* ``LW1000``
	* Description: emission levels in dB for 1000 Hz
	* Type: Double
* ``LW2000``
	* Description: emission levels in dB for 2000 Hz
	* Type: Double
* ``LW4000``
	* Description: emission levels in dB for 4000 Hz
	* Type: Double
* ``LW8000``
	* Description: emission levels in dB for 8000 Hz
	* Type: Double

.. note::
	You can define partially the bands. We defined here only octave bands but NoiseModelling is supporting third-octaves ``LW50, LW63, LW80, LW100, LW125, LW160, LW200, LW250, LW315, LW400, LW500, LW630, LW800, LW1000, LW1250, LW1600, LW2000, LW2500, LW3150, LW4000, LW5000, LW6300, LW8000, LW10000``.

