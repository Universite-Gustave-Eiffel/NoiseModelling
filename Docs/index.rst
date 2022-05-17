.. NoiseModelling documentation master file, created by
   sphinx-quickstart on Tue Oct  8 17:50:48 2019.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

NoiseModelling v4.0 User Guide
==========================================

.. figure:: images/Logo_noisemodelling.png
   :align: center

Welcome on the **official NoiseModelling v4.0 User Guide**.

NoiseModelling is a library capable of producing (regulatory) noise maps. 
It can be freely used either for research and education, as well as by experts in a professional use.

Since the release v4.0, this tool is fully compliant with the `CNOSSOS-EU`_ standard method for the noise emission (only road traffic) and `almost compliant`_ with noise propagation.

A general overview of the model (v3.4.5 - September 2020) can be found in `this video`_.


-  for **more information** on NoiseModelling, `visit the offical NoiseModelling website`_
-  to **contribute to NoiseModelling** source code, follow the ":doc:`Get_Started_Dev`" page
-  to **contact the development team**, 
    - open an issue : https://github.com/Ifsttar/NoiseModelling/issues or a write a message : https://github.com/Ifsttar/NoiseModelling/discussions *(we prefer these two options)*
    - send us an email at contact@noise-planet.org  

.. _CNOSSOS-EU : https://publications.jrc.ec.europa.eu/repository/handle/JRC72550

.. _almost compliant: Numerical_Model.html

.. _this video: https://www.youtube.com/watch?v=V1-niMT9cYE&t=1s

.. _visit the offical NoiseModelling website: http://noise-planet.org/noisemodelling.html


**Authors and licence**:

NoiseModelling is jointly developed by acousticians from the *Mixt Research Unit in Environmental Acoustics* (`UMRAE`_ - Université Gustave Eiffel & Cerema) and Geographic Information Science specialists from `Lab-STICC`_ laboratory (CNRS - DECIDE Team).

NoiseModelling and its documentation are distributed for free under GPL v3 :doc:`License`. 


.. _UMRAE: https://www.umrae.fr/

.. _Lab-STICC: https://labsticc.fr


**Publications**: 

NoiseModelling was initially developed in a research context, which has led to numerous scientific publications. For more information, have a look to ":doc:`Contributions`" page. 
To quote this tool, please use the bibliographic reference below:

.. note::
    Erwan Bocher, Gwenaël Guillaume, Judicaël Picaut, Gwendall Petit, Nicolas Fortin. *NoiseModelling: An Open Source GIS Based Tool to Produce Environmental Noise Maps*. ISPRS International Journal of Geo-Information, MDPI, 2019, 8 (3), pp.130. (`10.3390/ijgi8030130`_)


.. _10.3390/ijgi8030130: https://www.mdpi.com/2220-9964/8/3/130


**Fundings:**

*Research projects:*

- ANR Eval-PDU (ANR-08-VILL-0005) 2008-2011
- ANR Veg-DUD (ANR-09-VILL-0007) 2009-2014
- ANR CENSE (ANR-16-CE22-0012) 2017-2021
- Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
- PlaMADE 2020-2022

*Institutional (public) fundings:*

- `Université Gustave Eiffel`_ (formerly Ifsttar, formerly LCPC), `CNRS`_, `Cerema`_, `Université Bretagne Sud`_, `Ecole Centrale de Nantes`_

*Private fundings:*

- Airbus Urban Mobility


.. _Université Gustave Eiffel: https://www.univ-gustave-eiffel.fr/

.. _CNRS: https://www.cnrs.fr

.. _Cerema: https://www.cerema.fr/

.. _Université Bretagne Sud: https://www.univ-ubs.fr/

.. _Ecole Centrale de Nantes: https://www.ec-nantes.fr/

------------

.. warning::
    - The official documentation is available in English only
    -  Some illustrations may refer to previous versions of NoiseModelling
    -  If you observe some mistakes or errors, please open an issue `here`_ or contact us at contact@noise-planet.org
    -  You are also welcome to contribute to the documentation (click on *"Edit on Github"* - top of the page)


.. _here: https://github.com/Ifsttar/NoiseModelling/issues


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
