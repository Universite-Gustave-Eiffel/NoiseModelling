.. NoiseModelling documentation master file, created by
   sphinx-quickstart on Tue Oct  8 17:50:48 2019.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

NoiseModelling User Guide
==========================================

.. figure:: images/Logo_noisemodelling.png
   :align: center

This is the **official NoiseModelling User Guide**.

NoiseModelling is a library capable of producing noise maps of cities. This tool is `almost compliant`_ with the CNOSSOS-EU standard method for the noise emission (only road traffic) and noise propagation.
It can be freely used either for research and education, as well as by experts in a professional use.

A general overview of the model (September 2020) can be found at : https://www.youtube.com/watch?v=V1-niMT9cYE&t=1s

This plugin is distributed under `GPL 3 license`_ and is developed by the DECIDE team of the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics UMRAE (Ifsttar).

-  for **more information** on NoiseModelling, `visit the offical NoiseModelling website`_
-  to **contribute to NoiseModelling** from the source code, follow the instructions
-  to **contact the development team**, use the email contact@noise-planet.org or let an issue : https://github.com/Ifsttar/NoiseModelling/issues or a message : https://github.com/Ifsttar/NoiseModelling/discussions

**Cite as**: *Erwan Bocher, Gwenaël Guillaume, Judicaël Picaut, Gwendall Petit, Nicolas Fortin. NoiseModelling: An Open Source GIS Based Tool to Produce Environmental Noise Maps. ISPRS International Journal of Geo-Information, MDPI, 2019, 8 (3), pp.130. ⟨10.3390/ijgi8030130⟩. ⟨hal-02057736⟩*

**Fundings:**

*Research projects:*

- *ANR Eval-PDU (ANR-08-VILL-0005) 2008-2011*
- *ANR Veg-DUD (ANR-09-VILL-0007) 2009-2014*
- *ANR CENSE (ANR-16-CE22-0012) 2017-2021*
- *the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468*

*Institutional (public) fundings:*

- *Univ Gustave Eiffel (formerly Ifsttar, formerly LCPC), CNRS, UBS, ECN, Cerema*

*Private fundings:*

- *Airbus Urban Mobility*

.. note::
    - The official documentation is available in English only.
    -  Some illustrations may refer to previous versions of NoiseModelling.
    -  If you observe some mistakes or errors, please contact us at contact@noise-planet.org or let an issue `here`_.
    -  You can also contribute to the documentation
	 

.. _visit the offical NoiseModelling website: http://noise-planet.org/noisemodelling.html

.. _here: https://github.com/Ifsttar/NoiseModelling/issues

.. _almost compliant: Numerical_Model.html

.. _GPL 3 license: License.html

.. toctree::
    :maxdepth: 2
    :caption: NoiseModelling presentation
    
    Numerical_Model
    Validation
    Contributions

.. toctree::
    :maxdepth: 2
    :caption: Tutorials

    Get_Started_Tutorial
    Noise_Map_From_OSM_Tutorial
    Matsim_Tutorial
    Tutorials_FAQ

.. toctree::
    :maxdepth: 2
    :caption: User Interface

    WPS_Blocks
    FAQ_UF

.. toctree::
    :maxdepth: 2
    :caption: For Advanced Users

    Own_Wps
    dBeaver
    NoiseModellingScripting
    NoiseModellingOnPostGIS

.. toctree::
    :maxdepth: 2
    :caption: For Developers

    Get_Started_Dev

.. toctree::
    :maxdepth: 2
    :caption: Appendices

    Support
    License

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
