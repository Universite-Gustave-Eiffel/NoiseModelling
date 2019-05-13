# History

The NoiseModelling project started within the research project Eval-PDU, funded by the French National Research Agency (ANR) in 2009. During the project, it was decided to produce a tool that would be fully implemented in a GIS software, in order to produce noise maps for evaluating the noise impact on urban mobility plans. It was expected to propose an alternative of commercial software, as a free tool, which will be able to produce environmental noise maps on very large urban areas, requiring few computational resources, while providing relevant results. This tool was integrated in the OrbisGIS software and applied with success to the study the noise effect of several mobility plans of the Nantes metropolis.

Since this first study, many developments have been focused on NoiseModelling, in order to enhance both the noise calculation method and its integration within a full and simple system dedicating to environmental noise evaluation (data managing, noise exposure calculation, interactive noise maps...). The future of NoiseModelling is to become a reference tool for a large community (researchers, students, teachers, engineers...) in order to predict, to manage, to study... environmental noise.

# Principle

The noise calculation method implemented within the NoiseModelling plugin is based on the standard French method called NMPB 2008, as a reference method to be used under the Directive 2002/49/EC relating to the assessment and management of environmental noise (Read More).

Most of algorithms within the NoiseModelling plugin, mainly for the calculation of the sound propagation, are based on spatial analysis methods which allow to optimize and to reduce the complexity of the sound propagation path search in urban areas. Each part of the computation process has been split in several SQL functions thanks to the H2GIS database available within OrbisGIS. This open tool-box help the user to produce noise maps beyond the limitation of a monolithic computation software. However, the GIS environment allows to produce noise results with a high level of graphical representation, by coupling if necessary with other geospatial databases such as demographic data. For example, using GIS analysis methods, the user can easily compute a map of population exposure. 

# Architecture

NoiseModelling consists of two separated parts: **NoiseModelling core** and **NoiseModelling plugin**.

## NoiseModelling core

The **NoiseModelling core** contains the main processes to compute the sound level and to produce noise maps. The first part is related to the evaluation of the noise emission, by considering light and heavy vehicles, as well as trolley cars (road noise). The second part consists in evaluating the noise propagation from sources to receivers, according to the standard model described below. The main inputs of the propagation model are:
* an array of sounds sources defined by a geometry (lines or points) and a sound power for each third octave band from 100 Hz to 5000 Hz,
* an array of buildings (2D geometry),
* a list of calculation parameters, like the order of reflection and diffraction, wall absorption, maximum propagation distance... that are needed for the calculation of the sound attenuation between sources and the receivers.

This module written in JAVA is exposed as a set of SQL functions to the OrbisGIS software. It uses the relational database management system (RDMS) H2 and its spatial extension called H2GIS.

## NoiseModelling plugin

The **NoiseModelling plugin** offers an unique way to connect the NoiseModelling core processes within the OrbisGIS software, available as SQL functions. These functions are manipulated in a graphical user interface console that enables you to execute NoiseModelling commands. This console also proposes a number of features for helping with, code formatting, code writting (auto-completion, tooltips)...

However, the main added value is related to the GIS capabilities, such as map viewing, cartography, data edition, database access, which allow to interact with the input and output data. For example, the user can easily post-process the output data through the built-ins OrbisGIS functions, in order to:
* interpolate the receivers levels to build a continuous noise map (ISO levels),
* establish a spatial statistical study of the results in association with external data to compute population exposure,
* publish intermediate or final results as files and/or over networks services thanks to the OGC standards supported by OrbisGIS.
