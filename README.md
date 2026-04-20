[![CI](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/actions/workflows/CI.yml/badge.svg?branch=4.X)](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/actions/workflows/CI.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Documentation Status](https://readthedocs.org/projects/noisemodelling/badge/?version=latest)](https://noisemodelling.readthedocs.io/en/latest/?badge=latest)
[![GitHub release](https://img.shields.io/github/release/Universite-Gustave-Eiffel/NoiseModelling)](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/releases/)

<img src="https://noisemodelling.readthedocs.io/en/latest/_images/NoiseModelling_banner.png" width="90%">

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/company/noise-planet/)

NoiseModelling
===============

NoiseModelling is a free and open-source Java library for producing environmental noise maps from local to national scales, implementing CNOSSOS‑EU road and rail noise emission and propagation methods while linking to H2GIS and PostGIS for spatial analysis.

It responds to the need for robust noise assessment in public health and environmental planning by enabling simulation and prediction of noise propagation for mitigation design and compliance with EU regulations.

The software can be used independently or through a graphical interface and is openly available to the research, education, and professional communities.

It has been widely used for strategic noise mapping, dynamic maps driven by traffic models or sensors, sensitivity studies, and investigations of particular sources such as emergency sirens and drones.

Documentation
---------------------------

An online documentation is available : [NOISEMODELLING DOCUMENTATION](https://noisemodelling.readthedocs.io/en/latest/).

Here you'll find a wealth of useful information, including many step-by-step tutorials on how to use NoiseModelling.


Stable release
---------------------------

The current stable version of NoiseModelling can be found here: [latest release](https://github.com/Universite-Gustave-Eiffel/NoiseModelling/releases/latest)

Deployment on a public server
---------------------------

> NoiseModelling can be used as a local application, or it can be deployed on a server using Docker or Podman
> 
> The docker image is hosted on the Github Packages.
> 
> On the root of this repository you can find an example docker compose.
> 
> You can edit the following environment variables:
> 
> - PROXY_BASE_URL :  If you have a domain name you can use the your domain name instead of localhost
> - ROOT_URL : By default the service is accessible from the path /noisemodelling but you can change it by using the environment variable ROOT_URL (empty to use the base url)
> - UNSECURE_MODE : By default the registration is enabled (with TOTP). You can disable the registration by setting the environment variable UNSECURE_MODE in the docker-compose.yml file to true.
> 
> # Dependencies
> 
> Install Docker or Podman on your system
> 
> # Running
> 
> Download the file [docker-compose.yml](docker-compose.yml) and run this command in the same folder:
> 
> ```bash
> docker compose up -d
> ```
> 
> or
> 
> ```bash
> podman compose up -d
> ```
> 
> Follow the instructions of the logs in order to register the administrator account.
> 
> ```bash
> docker compose logs noisemodelling
> ```
> 
> or
> 
> ```bash
> podman compose logs noisemodelling
> ```




Contribute
---------------------------

To **contribute to NoiseModelling** source code, please read our [CONTRIBUTING](CONTRIBUTING.md) guide and the ["Get Started Dev"](https://noisemodelling.readthedocs.io/en/latest/Get_Started_Dev.html) page


Help & Support
---------------------------

To ask for help or contact the development team, you can either:

- open an issue : https://github.com/Universite-Gustave-Eiffel/NoiseModelling/issues or a write a message : https://github.com/Universite-Gustave-Eiffel/NoiseModelling/discussions *(we prefer these two options)*
- send us an email at ``contact@noise-planet.org``


Authors
---------------------------

NoiseModelling project is leaded by acousticians from the *Joint Research Unit in Environmental Acoustics* ([UMRAE](https://www.umrae.fr/), Université Gustave Eiffel - Cerema) and Geographic Information Science specialists from [Lab-STICC](https://labsticc.fr) laboratory (CNRS - DECIDE Team).

The NoiseModelling team owns the majority of the authorship of this application, but any external contributions are warmly welcomed.


Licence
---------------------------

NoiseModelling and its documentation are distributed for free and under the open-source [GPL v3](https://noisemodelling.readthedocs.io/en/latest/License.html) licence.


Publications & Fundings
--------------------------------------

* [Scientific production](https://noisemodelling.readthedocs.io/en/latest/Scientific_production.html)
* [Fundings](https://noisemodelling.readthedocs.io/en/latest/index.html#fundings)
