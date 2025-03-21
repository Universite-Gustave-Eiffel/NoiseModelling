Architecture
^^^^^^^^^^^^^^^^^

NoiseModelling is the name of the application that allows to calculate noise maps (notably through a Graphical User Interface). 
But did you know that it is also the name of different calculation libraries?

The documentation below presents the architecture of NoiseModelling with its different bricks and the ways to launch it:

#. NoiseModelling libraries
#. Database connection
#. NoiseModelling with a Graphical User Interface (GUI)
#. NoiseModelling with command line
#. NoiseModelling with Docker

.. figure:: images/Architecture/architecture.png
    :align: center
    :width: 100%


1. NoiseModelling libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling is made of 4 main `librairies`_: 

* ``noisemodelling-emission`` : to determine the noise emission
* ``noisemodelling-pathfinder`` : to determine the noise path
* ``noisemodelling-propagation`` : to calculate the noise propagation
* ``noisemodelling-jdbc`` : to connect NoiseModelling to a database

These libraries may be used independently of each other. Note that the ``noisemodelling-jdbc`` library *(JDBC = Java DataBase Connectivity)* is central since it allows the three others to communicate with each other as soon as the data are stored in a database *(which is the default situation)*.

.. _librairies: https://github.com/Ifsttar/NoiseModelling


2. Database connection
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Thanks to the ``noisemodelling-jdbc`` library, NoiseModelling can access and communicate with databases. This system is quite adapted to store, manage and process (spatial) data. Here, the user has the choice between to database (free, open-source and powerful) couples:

* `H2`_ / `H2GIS`_, which is configured and embeded by default. In this case, the user has nothing to do.
* `PostGreSQL`_ / `PostGIS`_. In this case, the user has to configure the connexion (read ":doc:`NoiseModellingOnPostGIS`" page for more information).

In both cases, database can be local or remote.


.. _H2 : https://www.h2database.com
.. _H2GIS: http://www.h2gis.org/
.. _PostgreSQL: https://www.postgresql.org/
.. _PostGIS: https://postgis.net/


3. NoiseModelling with a GUI
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

NoiseModelling has a Graphical User Interface (GUI). It is accessible through a web browser (here http://localhost:9580/geoserver/web/) and is generated by a module named ":doc:`WPS_Builder`".

In order for "WPS Builder" to communicate with the NoiseModelling libraries, we use a *'bridge'* named `GeoServer`_. This free and open-source software, allows (among other cool things) to execute WPS* scripts, written in `Groovy`_ language, via HTTP requests.

\* `Web Processing Service`_, which is a standard from the Open Geospatial Consortium (`OGC`_).


.. Note::
    When launching NoiseModelling, Geoserver is started first. In your terminal, you will have a lot of log messages. Most of them are coming from Geoserver and are not directly linked to NoiseModelling. Unfortunately, we can not remove them.

You can see NoiseModelling with a GUI in action in the page ":doc:`Get_Started_GUI`".


.. _GeoServer : http://geoserver.org/
.. _Groovy : http://www.groovy-lang.org/
.. _Web Processing Service : https://www.ogc.org/standards/wps
.. _OGC : https://www.ogc.org/


4. NoiseModelling with command lines
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can use NoiseModelling with command lines. To do so, 

#. Open a terminal
#. Go in the NoiseModelling directory
#. Call the WPS .groovy script you want, with the needed arguments


.. Note::
    The ``.groovy`` script may be simple (the ones already provided with NoiseModelling, executing one task) or complex (tailor made by users and calling one or many ``.groovy`` script(s)).


.. Note::
    No need to launch / start the application as we do with Geoserver. Here the NoiseModelling libraries are called directly for each instructions.

Examples can be found in the page ":doc:`Get_Started_Script`".


5. Docker Setup
~~~~~~~~~~~~~~~~~~~~

When a developer uses `Docker`_, he creates an application or service, which he then bundles together with the associated dependencies in a container image. An image is a static representation of the application or service, its configuration and dependencies.


Available versions
********************

The Docker images listed below have been built by NoiseModelling contributors / users. Many thanks to them!

* `v4.0.1 image`_, built by Alexander (Aka "`Xenotech81`_")
* `v3.4.4 image`_, built by Tomáš Anda (Aka "`tomasanda`_")

.. _Docker : https://www.docker.com/
.. _v4.0.1 image : https://hub.docker.com/r/xenotech/noisemodelling
.. _Xenotech81 : https://github.com/Xenotech81
.. _v3.4.4 image : https://github.com/tomasanda/docker-noisemodelling
.. _tomasanda : https://github.com/tomasanda