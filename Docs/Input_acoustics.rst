Acoustic parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In the different WPS scripts of NoiseModelling, you will find many input parameters, mandatory or optional. 

Below we list the most important ones, indicating, where necessary, the default values and those we recommend (from an acoustic point of view).

.. figure:: images/Input_tables/acoustics_parameters_banner.png
	:align: center



The following parameters may be found in the scripts dealing with noise emission or propagation (*e.g* ``Noise_level_from_traffic``, ``Noise_level_from_source```, ...)


Probability of occurrences
--------------------------------

* Parameter name: ``confFavorableOccurrencesXXXXX`` (with ``XXXXX`` = evening, day, night, ...)
* Description: Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one
* Type: Double
* Default value: ``0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5``
* Recommended value: 

.. figure:: images/Input_tables/acoustics_parameters_confFavorableOccurrences.png
	:align: center

Relative humidity
--------------------------------

* Parameter name: ``confHumidity``
* Description: Humidity for noise propagation (%) [0,100]
* Type: Double
* Default value: ``70``
* Recommended value: depends on the average conditions at the location where you perform the simulation

Air temperature
--------------------------------

* Parameter name: ``confTemperature``
* Description: Air temperature (°C)
* Type: Double
* Default value: ``15``
* Recommended value: depends on the average conditions at the location where you perform the simulation

Order of reflexion
--------------------------------

* Parameter name: ``confReflOrder``
* Description: Maximum number of reflections to be taken into account. Warning: adding 1 order increases the processing time significantly
* Type: Integer
* Default value: ``1``
* Recommended value: ``1`` or ``2``

Diffraction on horizontal edges
--------------------------------

* Parameter name: ``confDiffHorizontal``
* Description: Compute or not the diffraction on horizontal edges
* Type: Boolean
* Default value: ``False``
* Recommended value: ``True``

Diffraction on vertical edges
--------------------------------

* Parameter name: ``confDiffVertical``
* Description: Compute or not the diffraction on vertical edges. Following Directive 2015/996, enable this option for rail and industrial sources only
* Type: Boolean
* Default value: ``False``
* Recommended value: 

Maximum source-receiver distance
----------------------------------

* Parameter name: ``confMaxSrcDist``
* Description: Maximum distance between source and receiver (meters)
* Type: Double
* Default value: ``150``
* Recommended value: Between ``500`` and ``800``

.. figure:: images/Input_tables/acoustics_parameters_confMaxSrcDist.png
	:align: center

Maximum source-reflexion distance
------------------------------------

* Parameter name: ``confMaxReflDist``
* Description: Maximum search distance of walls / facades from the "Source-Receiver" segment, for the calculation of specular reflections (meters)
* Type: Double
* Default value: ``50``
* Recommended value: Between ``350`` and ``800``

.. figure:: images/Input_tables/acoustics_parameters_confMaxReflDist.png
	:align: center


Wall absorption coefficient
--------------------------------

* Parameter name: ``paramWallAlpha``
* Description: Wall absorption coefficient [0,1] (between ``0`` : "fully absorbent" and ``1`` : "fully reflective")
* Type: Double
* Default value: ``0.1``
* Recommended value: ``0.1``

Separate receiver level by source identifier
---------------------------------------------

* Parameter name: ``confExportSourceId``
* Description: Keep source identifier in output in order to get noise contribution of each noise source
* Type: Boolean
* Default value: ``False``
* Recommended value: 

Thread number
--------------------------------

* Parameter name: ``confThreadNumber``
* Description: Number of thread to use on the computer
* Type: Integer
* Default value: ``0`` (``0`` = Automatic. Will check the number of cores and apply -1. (*e.g*: 8 cores = 7 cores will be used))
* Recommended value: ``0``