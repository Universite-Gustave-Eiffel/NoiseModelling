NoiseModelling client line interface (CLI)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In this tutorial, we describe the different method to pilot NoiseModelling thanks to scripts. To do so, we will use a separate command-line interface, called ``ScriptRunner``, in which the GUI has been removed (no more  :doc:`Builder`).

From that point, NoiseModelling can be executed in 3 different manners:

#. with simple command lines
#. with Bash script
#. with Groovy script (Block)

To illustrate, users are invited to reproduce the tutorial ":doc:`Tutorial_Get_Started_GUI`" in command lines.

.. note::
    This tutorial is mainly dedicated to advanced users.
    
.. warning::
    The URL is here adapted to Linux or Mac users. Windows user may adapt the address by replacing ``/`` by ``\`` in the folders paths.

Requirements
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. warning::
    For all users (**Linux** , **Mac** and **Windows**), please make sure your Java environment is well configured. For more information, please read the page :doc:`Installation_guide`.


1. Simple command line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Using the terminal of your operating system

Below is an example of a bash instruction, executing the ``Noise_level_from_traffic.groovy`` script (located in the directory ``scripts/``). This block has 5 arguments corresponding to the input table names (for buildings, roads, receivers, dem and ground type).

.. literalinclude:: scripts/nm_terminal.bash
   :language: bash
   :linenos:

.. warning ::
   Adapt ``/home/user/NoiseModelling`` address with the real installation folder of NoiseModelling. Use the appropriate ``./bin/ScriptRunner`` or ``./bin/ScriptRunner.bat`` *(depending on if you are on Linux / Mac or Windows)* file, which is located in the ``bin/`` directory.


2. Bash script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a sequence of simple .groovy scripts (Blocks), using bash instructions and launching the steps described in the ":doc:`Tutorial_Get_Started_GUI`".

.. literalinclude:: scripts/get_started_tutorial_simple.bash
   :language: bash
   :linenos:


3. Groovy script (Block)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Below is an example of a complex .groovy script (Block), launching the different steps described in the ":doc:`Tutorial_Get_Started_GUI`".

.. literalinclude:: ../noisemodelling-scripts/src/test/groovy/org/noise_planet/noisemodelling/scripts/get_started_tutorial_complex.groovy
   :language: groovy
   :linenos:

You can find this script ``get_started_tutorial_complex.groovy`` on the installation folder of NoiseModelling

To run it use this bash command.

.. literalinclude:: scripts/run_get_started_tutorial_complex.bash
   :language: bash
   :linenos: