Pilot NoiseModelling with scripts
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this tutorial, we described the different way to pilot NoiseModelling without GUI and Geoserver.

To illustrate, users are invited to reproduce the tutorial ":doc:`Get_Started_GUI`"" in command lines.

This tutorial is mainly dedicated to advanced users.



Requirements: 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. warning::
    For all users (**Linux** , **Mac** and **Windows**), please make sure your Java environment is well setted. For more information, please read the page :doc:`Requirements`.


Simple command line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a bash instruction, executing the ``Noise_level_from_traffic.groovy`` WPS Script (located in the directory ``wps/``). This block has 5 arguments corresponding to the input table names (for buildings, roads, receivers, dem and ground type).

.. literalinclude:: scripts/nm_terminal.bash
   :language: bash
   :linenos:


``./bin/wps_scripts`` instruction allows to launch the ``wps_scripts.sh`` or ``wps_scripts.bat`` *(depending on if you are on Linux / Mac or Windows)* file, which is located in the ``bin/`` directory.


Bash script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a sequence of simple .groovy scripts, using bash instructions and launching the differents steps described in the ":doc:`Get_Started_GUI`".

.. literalinclude:: scripts/get_started_tutorial_simple.bash
   :language: bash
   :linenos:


Groovy script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a complex .groovy script, launching the differents steps described in the ":doc:`Get_Started_GUI`".

.. literalinclude:: scripts/get_started_tutorial_complex.groovy
   :language: java
   :linenos:



You can find this script online `here`_


.. _here : https://github.com/Ifsttar/NoiseModelling/pull/479/files#diff-904147c34e58c4be89782e57a27aae2ab1dab2e50c64ea9ad441609fa4fc909b
