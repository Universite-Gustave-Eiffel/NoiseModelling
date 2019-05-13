In 2017 NoiseM@p become Noise Modelling for the integration under the same umbrella project http://noise-planet.org .

NoiseModelling is a plugin of the [OrbisGIS](http://www.orbisgis.org) software (an open-source geographic information system) that able to produce noise maps of cities, according to the french standard method for the road noise [emission][nmpb_E] and using the [NMPB][nmpb_P] method for the sound propagation.

[nmpb_E]: http://www.infra-transports-materiaux.cerema.fr/IMG/pdf/0924-1A_Road_noise_prediction_v1.pdf "Road noise prediction. Part 1 - Calculating sound emissions from road traffic, SETRA (2009)"
[nmpb_P]: http://www.setra.developpement-durable.gouv.fr/IMG/pdf/US_0957-2A_Road_noise_predictionDTRF.pdf "Road noise prediction. Part 2 - Noise propagation computation method including meteorological effects (NMPB 2008), SETRA (2009)"

* [[Overview|00-Overview]] - An overview of the project history and of the plugin architecture
* [[Installation|01-Installation]] - Install OrbisGIS and the NoiseModelling plugin
* [[Quick Start|02-Quick-Start]] - Demonstration without input data
* [[SQL functions|03-SQL-functions]] - Description of the SQL functions

Applications:

* [[Industrial sound sources|04-Industrial-sound-sources-application]] - Use NoiseModelling for industrial sound sources (punctual sound sources)
* [[Ground effect|05-Ground-effect]] - Consider ground reflection in calculation
* [[Using OpenStreetMap|09-Using-data-from-OpenStreetMap]] - Use geospatial data from OpenStreetMap
* [[Horizontal noise map|07-Horizontal-noise-map]] - Make a horizontal noise map
* [[Vertical noise map|08-Vertical-noise-map]] - Make a vertical cut of a noise map

Using Groovy (H2GIS or PostGIS):

* [[Digital elevation model|06 Modelling-barrier-using-digital-elevation-model]] - Make noise map with digital elevation model
* [[Scripting with Groovy|10-Scripting-with-Groovy]] - Use NoiseModelling without Graphical User Interface

Using Python (H2GIS):

* [[Scripting with Python|11-Scripting-with-Python]] - Use NoiseModelling without Graphical User Interface
