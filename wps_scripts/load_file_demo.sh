#! /bin/bash

# create (or load existing) database and load a shape file into the database
./bin/wps_scripts -w./ -snoisemodelling/wps/Import_and_Export/Import_File.groovy pathFile=resources/org/noise_planet/noisemodelling/wps/buildings.shp

