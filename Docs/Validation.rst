Validation
^^^^^^^^^^^^^

Acoustic model validation
---------------------------

Please refer to `CNOSSOS-EU`_ papers, or other scientific papers, which are independant from NoiseModelling.

Some limits are given in the CNOSSOS-EU documents below:


.. note::
    Source : Stylianos Kephalopoulos, Marco Paviotti, Fabienne Anfosso-Lédée. `Common noise assessment methods in Europe (CNOSSOS-EU)`_. PUBLICATIONS OFFICE OF THE EUROPEAN UNION, p.75/180 2012,10.2788/31776

    * Height receivers must be > 2m
    * Propagation distance must be < 800 m
    * Downward‐refraction/ homogeneous are taken into acount
    * 63 Hz to 4 000 Hz – center band
    * Breakdown of the infrastructures into point sources
    * Does not apply to propagation scenarios above a water body (lake, wide river, etc.).
    * The effects of tunnel mouths are not dealt with by the method.
    * This method considers obstacles to be equivalent to flat surfaces. 


.. _Common noise assessment methods in Europe (CNOSSOS-EU) : https://hal.archives-ouvertes.fr/hal-00985998/document

.. note::
    Source : https://circabc.europa.eu/sd/a/9566c5b9-8607-4118-8427-906dab7632e2/Directive_2015_996_EN.pdfde

    This document specifies a method for calculating the attenuationComputeOutput of noise during its outdoor propagation.
    Knowing the characteristics of the source, this method predicts the equivalent continuous sound pressure level at a receiver point corresponding to two particular types of atmospheric conditions: 

    * downward-refraction propagation conditions (positive vertical gradient of effective sound celerity) from the source to the receiver
    * homogeneous atmospheric conditions (null vertical gradient of effective sound celerity) over the entire area of propagation.

    The method of calculation described in this document applies to industrial infrastructures and land transport 	infrastructures. It therefore applies in particular to road and railway infrastructures. Aircraft transport is included in the scope of the method only for the noise produced during ground operations and excludes take-off and landing.

    Industrial infrastructures that emit impulsive or strong tonal noises as described in ISO 1996-2:2007 do not fall within the scope of this method.

    The method of calculation does not provide results in upward-refraction propagation conditions (negative vertical gradient of effective sound speed) but these conditions are approximated by homogeneous conditions when computing Lden.

    To calculate the attenuationComputeOutput due to atmospheric absorption in the case of transport infrastructure, the temperature and humidity conditions are calculated according to ISO 9613-1:1996.

    The method provides results per octave band, from 63 Hz to 8 000 Hz. The calculations are made for each of the centre frequencies.

    Partial covers and obstacles sloping, when modelled, more than 15° in relation to the vertical are out of the scope of this calculation method.

    A single screen is calculated as a single diffraction calculation, two or more screens in a single path are treated as a subsequent set of single diffractions by applying the procedure described further.






Implementation validation
--------------------------

A large set of unit tests are present in the `code`_. Please consult an example dealing with CNOSSOS-EU `here`_. 

Note that all the tests entilted ``TCxx`` (`see example`_) are coming from the `ISO/TR 17534-4:2020`_ standard , which has been implemented in NoiseModelling.



.. _CNOSSOS-EU: https://circabc.europa.eu/sd/a/9566c5b9-8607-4118-8427-906dab7632e2/Directive_2015_996_EN.pdfde

.. _code: https://github.com/Ifsttar/NoiseModelling/

.. _here: https://github.com/Ifsttar/NoiseModelling/blob/4.X/noisemodelling-jdbc/src/test/java/org/noise_planet/noisemodelling/jdbc/EvaluateAttenuationCnossosTest.java

.. _see example: https://github.com/Ifsttar/NoiseModelling/blob/621ec99568ac14d72ef78557cfc2ee910a72c138/noisemodelling-jdbc/src/test/java/org/noise_planet/noisemodelling/jdbc/EvaluateAttenuationCnossosTest.java#L453

.. _ISO/TR 17534-4:2020 : https://www.iso.org/standard/72115.html