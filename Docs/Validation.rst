Validation
^^^^^^^^^^^^^

Numerical model validation
---------------------------

Please refer to `CNOSSOS-EU`_ papers, which is independant from NoiseModelling.


Implementation validation
--------------------------

A large set of unit tests are present in the `code`_. Please consult an example dealing with CNOSSOS-EU `here`_. 

Note that all the tests entilted ``TCxx`` (`see example`_) are coming from the `ISO/TR 17534-4:2020`_ standard.



.. _CNOSSOS-EU: https://circabc.europa.eu/sd/a/9566c5b9-8607-4118-8427-906dab7632e2/Directive_2015_996_EN.pdfde

.. _code: https://github.com/Ifsttar/NoiseModelling/

.. _here: https://github.com/Ifsttar/NoiseModelling/blob/4.X/noisemodelling-jdbc/src/test/java/org/noise_planet/noisemodelling/jdbc/EvaluateAttenuationCnossosTest.java

.. _see example: https://github.com/Ifsttar/NoiseModelling/blob/621ec99568ac14d72ef78557cfc2ee910a72c138/noisemodelling-jdbc/src/test/java/org/noise_planet/noisemodelling/jdbc/EvaluateAttenuationCnossosTest.java#L453

.. _ISO/TR 17534-4:2020 : https://www.iso.org/standard/72115.html