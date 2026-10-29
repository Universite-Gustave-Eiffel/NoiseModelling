/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.scripts.NoiseModelling

import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.propagation.AttenuationParameters
import org.noise_planet.noisemodelling.propagation.DiscreteFavourableProbability
import org.noise_planet.noisemodelling.propagation.DutchFavourableProbabilityFactory

import java.sql.Connection

title = 'Generate default atmospheric settings from the PERIOD field of a noise emission table'
description = '&#10145;&#65039; Generate default atmospherics settings from the PERIOD field of a noise emission table.' +
        ' It is used to export the result table to be edited and reimported to be used into Noise_level_from_source.' +
        ' This table make you able to change the temperature and other settings for each time period of the simulation'

inputs = [
        tableSourcesEmission            : [
                name       : 'Sources emission table name',
                title      : 'Sources emission table name',
                description: 'Name of the Sources table (ex. SOURCES_EMISSION) </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li><b> IDSOURCE </b>* : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)</li>' +
                        '<li><b> PERIOD </b>* : Time period, you will find this column on the output (VARCHAR)</li>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confDutchFraction: [
                name       : 'Dutch favourable fraction',
                title      : 'Dutch favourable fraction',
                description: 'Use the Dutch formulas on calculating the ratio for favourable/homogenous propagation',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        confHumidity            : [
                name       : 'Relative humidity',
                title      : 'Relative humidity',
                description: '&#127783; Humidity for noise propagation (%) [0,100]',
                default    : 70,
                type       : Double.class
        ],
        confTemperature         : [
                name       : 'Temperature',
                title      : 'Air temperature',
                description: '&#127777; Air temperature (°C)',
                default    : 15,
                type       : Double.class
        ],
        confFavourableOccurrencesDefault: [
                name       : 'Default favourable occurrences',
                title      : 'Default favourable occurrences',
                description: 'Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one. </br> </br>' +
                        '<img src="wps_images/acoustics_parameters_confFavorableOccurrences.png" alt="Noise level from source" width="95%" align="center">. For Netherlands check confDutchFraction instead of using this parameter.',
                default    : '',
                min        : 0, max: 1,
                type       : String.class
        ],
        tablePeriodAtmosphericSettings          : [
                name       : 'Output table name',
                title      : 'Output table name',
                description: 'Name of the Atmospheric settings table </br> </br>' +
                        'The table will contain the following columns: </br> <ul>' +
                        '<li> <b> PERIOD </b>: time period (VARCHAR PRIMARY KEY) </li> ' +
                        '<li> <b> WINDROSE </b>: Comma-delimited string containing the probability ([0,1]) of occurrences of favourable propagation conditions. Follow the clockwise direction. The north slice is the last array index (n°16 in the schema below) not the first one. <img src="wps_images/acoustics_parameters_confFavorableOccurrences.png" alt="Noise level from source" width="95%" align="center"> or DutchD, DutchE, DutchN for Netherlands </li> ' +
                        '<li> <b> TEMPERATURE </b>: Temperature in celsius (FLOAT) </li> ' +
                        '<li> <b> PRESSURE </b>: air pressure in pascal (FLOAT) </li> ' +
                        '<li> <b> HUMIDITY </b>: air humidity in percentage (FLOAT) </li> ' +
                        '<li> <b> GDISC </b>: choose between accept G discontinuity or not (BOOLEAN) default true </li> ' +
                        '<li> <b> PRIME2520 </b>: choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false </li> ',
                default   : 'SOURCES_ATMOSPHERIC',
                min        : 0, max: 1,
                type: String.class
        ],
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]


// main function of the script
def exec(Connection connection, Map input) {

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    def tablePeriodAtmosphericSettings = "SOURCES_ATMOSPHERIC"

    if(input.containsKey("tablePeriodAtmosphericSettings")) {
        tablePeriodAtmosphericSettings = input.get("tablePeriodAtmosphericSettings") as String
    }

    boolean outputDutchFraction = false;
    if(input.containsKey("confDutchFraction")) {
        outputDutchFraction = input.get("confDutchFraction") as boolean;
    }

    double defaultTemperature = input.getOrDefault("confTemperature", 15) as double
    double defaultHumidity = input.getOrDefault("confHumidity", 70) as double
    String defaultFavourableOccurrences = input.getOrDefault("confFavourableOccurrencesDefault", "") as String

    AttenuationParameters defaultParameters = new AttenuationParameters()

    if(!outputDutchFraction && !defaultFavourableOccurrences.isEmpty()) {
        defaultParameters.setWindRose(new DiscreteFavourableProbability(defaultFavourableOccurrences.split(",").collect { it.trim() as double }))
    }
    defaultParameters.setTemperature(defaultTemperature)
    defaultParameters.setHumidity(defaultHumidity)

    List<String> periods = Arrays.asList("D", "E", "N")

    if(input.containsKey("tableSourcesEmission")) {
        String tableSourcesEmission = input.get("tableSourcesEmission") as String
        if(!JDBCUtilities.tableExists(connection, tableSourcesEmission)) {
            throw new IllegalArgumentException("Table does not exist: " + tableSourcesEmission)
        }
        periods = JDBCUtilities.getUniqueFieldValues(connection, tableSourcesEmission, "PERIOD")
    }

    periods.each { String period ->
        if(outputDutchFraction) {
            switch (period) {
                case "D":
                    defaultParameters.setWindRose(DutchFavourableProbabilityFactory.getFavourableProbabilityGenerator("DutchD"))
                    break;
                case "E":
                    defaultParameters.setWindRose(DutchFavourableProbabilityFactory.getFavourableProbabilityGenerator("DutchE"))
                    break;
                case "N":
                    defaultParameters.setWindRose(DutchFavourableProbabilityFactory.getFavourableProbabilityGenerator("DutchN"))
                    break;
            }
        }
        defaultParameters.writeToDatabase(connection, tablePeriodAtmosphericSettings, period);
    }

    return [result: tablePeriodAtmosphericSettings]
}
