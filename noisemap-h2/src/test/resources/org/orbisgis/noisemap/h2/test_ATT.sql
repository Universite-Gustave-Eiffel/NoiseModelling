
CALL SHPREAD('D:/aumond/Documents/github/NoiseModelling/noisemap-h2/src/test/resources/org/orbisgis/noisemap/h2/buildings_zone_capteur2.shp','buildings_zone_capteur2');
CALL SHPREAD('D:/aumond/Documents/github/NoiseModelling/noisemap-h2/src/test/resources/org/orbisgis/noisemap/h2/CARS_1700.shp','CARS_1700');
CALL SHPREAD('D:/aumond/Documents/github/NoiseModelling/noisemap-h2/src/test/resources/org/orbisgis/noisemap/h2/REC_142.shp','rec_142');

create table ATT_CARS as SELECT * from BR_PTGRID3D_ATT_f('buildings_zone_capteur2', 'HAUTEUR', 'CARS_1700', 'REC_142', 'DB_M','', '', 750, 100, 0, 0, 0.1); --- xxxxx seconds