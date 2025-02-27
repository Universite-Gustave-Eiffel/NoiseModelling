Pilot NoiseModelling with scripts
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this tutorial, we describe the different ways to pilot NoiseModelling thanks to scripts. To do so, we will use a dedicated packaging of NoiseModelling, called ``NoiseModelling_5.0.0_without_gui``, in which the GUI has been removed (no more Geoserver and :doc:`WPS_Builder`).

#. Go to the NoiseModelling latest `release page`_
#. Download and unzip the `NoiseModelling_5.0.0_without_gui`_ file

.. _release page : https://github.com/Ifsttar/NoiseModelling/releases/latest
.. _NoiseModelling_5.0.0_without_gui : https://github.com/Ifsttar/NoiseModelling/releases/download/v5.0.0/NoiseModelling_5.0.0_without_gui.zip

From that point, NoiseModelling can be executed in 3 different manners:

#. with simple command lines
#. with Bash script
#. with Groovy script

To illustrate, users are invited to reproduce the tutorial ":doc:`Get_Started_GUI`" in command lines.

.. note::
    This tutorial is mainly dedicated to advanced users.
    
.. warning::
    The URL is here adapted to Linux or Mac users. Windows user may adapt the address by replacing ``/`` by ``\`` and the drive name.

Requirements
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. warning::
    For all users (**Linux** , **Mac** and **Windows**), please make sure your Java environment is well setted. For more information, please read the page :doc:`Requirements`.


1. Simple command line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Using the terminal of your operating system (Java 11 must be in the system path)

Below is an example of a bash instruction, executing the ``Noise_level_from_traffic.groovy`` WPS Script (located in the directory ``/noisemodelling/wps/``). This block has 5 arguments corresponding to the input table names (for buildings, roads, receivers, dem and ground type).

.. literalinclude:: scripts/nm_terminal.bash
   :language: bash
   :linenos:


``./bin/wps_scripts`` instruction allows to launch the ``wps_scripts.sh`` or ``wps_scripts.bat`` *(depending on if you are on Linux / Mac or Windows)* file, which is located in the ``bin/`` directory.


.. warning ::
   Adapt ``/home/user/`` address with your own situation


2. Bash script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a sequence of simple .groovy scripts, using bash instructions and launching the steps described in the ":doc:`Get_Started_GUI`".

.. literalinclude:: scripts/get_started_tutorial_simple.bash
   :language: bash
   :linenos:


3. Groovy script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a complex .groovy script, launching the differents steps described in the ":doc:`Get_Started_GUI`".

.. literalinclude:: scripts/get_started_tutorial_complex.groovy
   :language: java
   :linenos:

You can find this script online `here`_

.. _here : https://github.com/Ifsttar/NoiseModelling/blob/master/wps_scripts/src/main/groovy/get_started_tutorial.groovy
