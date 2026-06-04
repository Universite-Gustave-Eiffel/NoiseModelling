#! /bin/bash

# Run the get started turorial
# https://noisemodelling.readthedocs.io/en/latest/Get_Started_Tutorial.html

# Step 4: Upload files to database
# create (or load existing) database and load a shape file into the database
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Import_File.groovy --pathFile resources/ground_type.shp
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Import_File.groovy --pathFile resources/buildings.shp
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Import_File.groovy --pathFile resources/receivers.shp
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Import_File.groovy --pathFile resources/ROADS2.shp
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Import_File.groovy --pathFile resources/dem.geojson


# Step 5: Run Calculation
./bin/ScriptRunner -w ./ -s scripts/NoiseModelling/Noise_level_from_sources.groovy --tableBuilding BUILDINGS --tableSources ROADS2 --tableReceivers RECEIVERS --tableDEM DEM --tableGroundAbs GROUND_TYPE

# Step 6: Export (& see) the results
./bin/ScriptRunner -w ./ -s scripts/Import_and_Export/Export_Table.groovy --exportPath RECEIVERS_LEVEL.shp --tableToExport RECEIVERS_LEVEL
