The processing is done through SQL requests with the following SQL functions:

## BR_EvalSource

Return the dB(A) global value of equivalent source power of combined light and heavy traffic.

1. BR_EvalSource(double speed_load, int lv_per_hour, int hv_per_hour)
2. BR_EvalSource(double speed_load, int lv_per_hour, int hv_per_hour, double beginZ, double endZ,double road_length_2d)
3. BR_EvalSource(double speed_load, int lv_per_hour, int hv_per_hour, double speed_junction, double speed_max,int copound_roadtype,  double beginZ, double endZ, double roadLength2d, boolean isQueue)
4. BR_EvalSource(double lv_speed, double hv_speed,int vl_per_hour, int pl_per_hour, double beginZ, double endZ,double road_length_2d)

The function 3 evaluate the hv_speed using speed_junction, speed_max, copound_roadtype and isQueue.

The function 4 is the complete evaluation function without default parameters.

Parameters:

 - **speed_load** Average speed of vehicles.
 - **lv_per_hour** Average light vehicle by hour
 - **hv_per_hour** Average heavy vehicle by hour
 - **beginZ** Beginning of road height. Used to compute slope.
 - **endZ** End of road height. Used to compute slope.
 - **road_length_2d** 2D length of road. Used to compute slope.
 - **speed_junction** Speed of vehicle at road junction.
 - **speed_max** Legal maximum speed of the road.
 - **copound_roadtype** Road type:
`10` Highway 2x2 130 km/h
`21` 2x2 way 110 km/h
`22` 2x2 way 90km/h off belt-way
`23` Belt-way
`31` Interchange ramp
`32` Off boulevard roundabout circular junction
`37` Inside-boulevard roundabout circular junction
`41` lower level 2x1 way 7m 90km/h
`42` Standard 2x1 way 90km/h
`43` 2x1 way
`51` extra boulevard 70km/h
`52` extra boulevard 50km/h
`53` extra boulevard Street 50km/h
`54` extra boulevard Street <50km/h
`56` in boulevard 70km/h
`57` in boulevard 50km/h
`58` in boulevard Street 50km/h
`59` in boulevard Street <50km/h
`61` Bus-way boulevard 70km/h
`62` Bus-way boulevard 50km/h
`63` Bus-way extra boulevard Street
`64` Bus-way extra boulevard Street
`68` Bus-way in boulevard Street 50km/h
`69` Bus-way in boulevard Street <50km/h
 - **isQueue** If this segment of road is behind a traffic light. If vehicles behavior is to stop at the end of the road.
 - **lv_speed** Average light vehicle speed
 - **hv_speed** Average heavy vehicle speed

## BTW_EvalSource

Return the dB(A) global value of equivalent source power of tramway traffic.

1. BTW_EvalSource(double speed, double tw_per_hour, int groundType, boolean has_anti_vibration)

 - **speed** Average speed
 - **tw_per_hour** Average tramway by hour
 - **groundType** Ground type 0:Grass 1:Rigid
 - **has_anti_vibration** True if Anti-vibration system is installed


## BR_SpectrumRepartition

Return the dB(A) level of third-octave frequency band using light/heavy vehicle emission spectrum.

1. BR_SpectrumRepartition(int freqBand, int roadSurf, double level)

 - **freqBand** One of 100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000
 - **roadSurf** Only surface 1 is handled
 - **level** Global dB(A) value
 
## BTW_SpectrumRepartition

Return the dB(A) level of third-octave frequency band using tramway emission spectrum.

BTW_SpectrumRepartition(int freqBand, double level)

 - **freqBand** One of 100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000
 - **level** Global dB(A) value

## BR_TriGrid

Table function.Sound propagation in 2 dimension. Return 6 columns. TRI_ID integer,THE_GEOM polygon,W_V1 double,W_V2 double,W_V3 double,CELL_ID integer.
 
BR_TriGrid(Geometry envelopeGeometry, VARCHAR buildingsTable, VARCHAR sourcesTable,VARCHAR sourcesTableSoundFieldName, VARCHAR groundTypeTable, double maximumPropagationDistance, double maximumWallSeekingDistance, double roadsWidth, double receiversDensification, double maximumAreaOfTriangle, int soundReflectionOrder, int soundDiffractionOrder, double wallAlpha)
 
 - **computeEnvelope** (Optionnal) Envelope of the receiver domain to compute. Without this parameter the envelope is the extent of the source table.
 - **buildingsTable** table identifier that contain a geometry column of type POLYGON.
 - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The table must contain the sound emission level in dB(A).
 - **sourcesTableSoundFieldName** prefix identifier of the emission level column. ex 'DB_M' for columns 'DB_M100' to 'DB_M5000'.  
 - **groundTypeTable** table identifier of the ground category table. This table must contain a geometry field of type POLYGON. And a column 'G' of type double between 0 and 1.
 dimensionless coefficient G:
    - Law, meadow, field of cereals G=1
    - Undergrowth (resinous or decidious) G=1
    - non-compacted earth G=0.7
    - Compacted earth, track G=0.3
    - Road surface G=0
    - Smooth concrete G=0
 - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and memory usage.
 - **maximumWallSeekingDistance** From the direct propagation line source-receiver, wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts performance.
 - **roadsWidth** Start creating receivers from this distance. Should be superior than 1 meter.
 - **receiversDensification** Create additional receivers at this distance from sources. (0 to disable)
 - **maximumAreaOfTriangle** Maximum area for noise map triangular mesh. Smaller area means more receivers. Impacts performance.
 - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. Recommended value is 2.
 - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended value is 1.
 - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete.
 
## BR_TriGrid3D

Table function.Sound propagation in 3 dimension. Return 6 columns. TRI_ID integer,THE_GEOM polygon,W_V1 double,W_V2 double,W_V3 double,CELL_ID integer.

BR_TriGrid3D(Geometry computeEnvelope,VARCHAR buildingsTable, VARCHAR heightFieldName, VARCHAR sourcesTable, VARCHAR sourcesTableSoundFieldName, VARCHAR groundTypeTable, VARCHAR demTable, double maximumPropagationDistance, double maximumWallSeekingDistance, double roadsWidth, double receiversDensification, double maximumAreaOfTriangle, int soundReflectionOrder, int soundDiffractionOrder, double wallAlpha)
 
 - **computeEnvelope** (Optionnal) Envelope of the receiver domain to compute. Without this parameter the envelope is the extent of the source table.
 - **buildingsTable** table identifier that contain a geometry column of type POLYGON. Polygon Z value is the ground level.
 - **heightFieldName** column identifier in the buildings table that hold building height in meter.
 - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The table must contain the sound emission level in dB(A).Caution. The Z value is absolute. The source or receptors below the topology of the terrain are ignored. The source table must contain a primary key.
 - **sourcesTableSoundFieldName** prefix identifier of the emission level column. ex 'DB_M' for columns 'DB_M100' to 'DB_M5000'.  
 - **groundTypeTable** table identifier of the ground category table. This table must contain a geometry field of type POLYGON. And a column 'G' of type double between 0 and 1.
 dimensionless coefficient G:
    - Law, meadow, field of cereals G=1
    - Undergrowth (resinous or decidious) G=1
    - non-compacted earth G=0.7
    - Compacted earth, track G=0.3
    - Road surface G=0
    - Smooth concrete G=0
 - **demTable** table identifier that contain the digital elevation model. A geometry column of type POINT with X,Y and Z value. 
 - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and memory usage.
 - **maximumWallSeekingDistance** From the direct propagation line source-receiver, wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts performance.
 - **roadsWidth** Start creating receivers from this distance. Should be superior than 1 meter.
 - **receiversDensification** Create additional receivers at this distance from sources. (0 to disable)
 - **maximumAreaOfTriangle** Maximum area for noise map triangular mesh. Smaller area means more receivers. Impacts performance.
 - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. Recommended value is 2.
 - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended value is 1.
 - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete.

## BR_PtGrid

Sound propagation but the user provide the receiver coordinate (receiversTable). This receiver table must provide an integer primary key in order to be linked with the result table that provide a corresponding identifier.

BR_PtGrid(VARCHAR buildingsTable,VARCHAR sourcesTable, VARCHAR receiversTable, VARCHAR sourcesTableSoundFieldName, VARCHAR groundTypeTable,VARCHAR demTable, double maximumPropagationDistance, double maximumWallSeekingDistance, int soundReflectionOrder, int soundDiffractionOrder, double wallAlpha)

 - **buildingsTable** table identifier that contain a geometry column of type POLYGON.
 - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The table must contain the sound emission level in dB(A). The source table must contain a primary key.
 - **receiversTable** table identifier that contain the list of evaluation point of sound level. This table must contains only POINT. And optionally an integer primary key.
 - **sourcesTableSoundFieldName** prefix identifier of the emission level column. ex 'DB_M' for columns 'DB_M100' to 'DB_M5000'.  
 - **groundTypeTable** table identifier of the ground category table. This table must contain a geometry field of type POLYGON. And a column 'G' of type double between 0 and 1.
 dimensionless coefficient G:
    - Law, meadow, field of cereals G=1
    - Undergrowth (resinous or decidious) G=1
    - non-compacted earth G=0.7
    - Compacted earth, track G=0.3
    - Road surface G=0
    - Smooth concrete G=0
 - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and memory usage.
 - **maximumWallSeekingDistance** From the direct propagation line source-receiver, wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts performance.
 - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. Recommended value is 2.
 - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended value is 1.
 - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete.

## BR_PtGrid3D

Sound propagation but the user provide the receiver coordinate (receiversTable). This receiver table must provide an integer primary key in order to be linked with the result table that provide a corresponding identifier.

BR_PtGrid3D(VARCHAR buildingsTable, VARCHAR heightFieldName,VARCHAR sourcesTable, VARCHAR receiversTable, VARCHAR sourcesTableSoundFieldName, VARCHAR groundTypeTable, double maximumPropagationDistance, double maximumWallSeekingDistance, int soundReflectionOrder, int soundDiffractionOrder, double wallAlpha)

 - **buildingsTable** table identifier that contain a geometry column of type POLYGON. A digital elevation model is derived from the Z coordinates of the buildings polygons.
 - **heightFieldName** column identifier in the buildings table that hold building height in meter. 
 - **sourcesTable** table identifier that contain a geometry column of type POINT or LINESTRING.The table must contain the sound emission level in dB(A). Caution. The Z value is absolute. The source or receptors may be below the topology of the terrain.
 - **receiversTable** table identifier that contain the list of evaluation point of sound level. This table must contains only POINT. And optionally an integer primary key.Caution. The Z value is absolute. The source or receptors may be below the topology of the terrain.
 - **sourcesTableSoundFieldName** prefix identifier of the emission level column. ex 'DB_M' for columns 'DB_M100' to 'DB_M5000'.  
 - **groundTypeTable** table identifier of the ground category table. This table must contain a geometry field of type POLYGON. And a column 'G' of type double between 0 and 1.
 dimensionless coefficient G:
    - Law, meadow, field of cereals G=1
    - Undergrowth (resinous or decidious) G=1
    - non-compacted earth G=0.7
    - Compacted earth, track G=0.3
    - Road surface G=0
    - Smooth concrete G=0
 - **demTable** table identifier that contain the digital elevation model. A geometry column of type POINT with X,Y and Z value.    
 - **maximumPropagationDistance** From a receiver, each source that are farther than this parameter are ignored. Recommended value, greater or equal to 750 meters. Greatly impacts performance and memory usage.
 - **maximumWallSeekingDistance** From the direct propagation line source-receiver, wall farther than this parameter are ignored for reflection and diffraction. Greatly impacts performance.
 - **soundReflectionOrder** Maximum depth of wall reflection. Greatly impacts performance. Recommended value is 2.
 - **soundDiffractionOrder** Maximum depth of sound diffraction. Impacts performance. Recommended value is 1.
 - **wallAlpha** Wall absorption value. Between 0 and 1. Recommended value is 0.23 for concrete.
