/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
import org.orbisgis.core.ui.plugins.views.beanShellConsole.commands.SQL;
import java.io.*;

String sqlCode="";
boolean computeMainRoads=true;
boolean computeSecondaryRoads=true;
boolean computeTramway=true; //tramway
boolean computeBusWay=true; //busway
boolean computeBus=true; //public bus
boolean computeLDEN=true;
String[] daytimes={"hpm_","hcj_","hps_","hcn_"};
String[] public_daytimes={"hpm_","hcj_","hps_","hcn_","hcn2_"};
String[] mode_transport={"private","bus","tram","busway"};



if(computeMainRoads || computeSecondaryRoads || computeBus) {
    // Send the Z coordinate from "BD_TOPO" to the traffic geometries
    // Simplify line segments and filter informations
    sqlCode+="DROP TABLE IF EXISTS Simplified_Reseau_polyline;\n";
    sqlCode+="CREATE TABLE Simplified_Reseau_polyline AS SELECT ST_Simplify(the_geom,0.8) as the_geom, NUM, ORI,EXT FROM ST_SetNearestZ( Reseau_polyline,route, 200 );\n";
}
if(computeSecondaryRoads) {
    // Filter columns and rows
    sqlCode+="create table roads AS SELECT the_geom,ID FROM route WHERE  NATURE!='Bac piéton'  AND NATURE!='Bac auto' AND NATURE!='Chemin' AND NATURE!='Escalier' AND NATURE!='Piste cyclabe' AND NATURE!='Sentier';\n";
    // Link traffic area IDS with roads
    sqlCode+="create table secondary_roads_with_areaid as select the_geom,ID,ID2 as NUMERO from ST_SetNearestGeometryId(roads,zonage,'the_geom','the_geom', 'NUMERO' );\n";
    sqlCode+="drop table roads purge;\n";
    // Count the number of roads contained in each zone
    sqlCode+="DROP TABLE IF EXISTS area_roads_count purge;\n";
    sqlCode+="create table area_roads_count as select NUMERO as num_zone,COUNT(*) as roadcpt FROM secondary_roads_with_areaid GROUP BY NUMERO;\n";
}

for(int id=0;id<daytimes.length;id++) {
    String dayt=daytimes[id];
    if(computeMainRoads) {
        // Link roads with queue database, split geometry corresponding to the two directions
        sqlCode+="CREATE TABLE Queue_Reseau_polyline_Dir_One AS SELECT srp.the_geom,id_tronc,from_node,to_node,speed_max,speed_load,queue.sd_cross as speed_cross,queue_len*1000 as queue_length FROM Simplified_Reseau_polyline as srp,"+dayt+"queue as queue WHERE srp.ORI=queue.from_node AND srp.EXT=queue.to_node;\n";
        sqlCode+="CREATE TABLE Queue_Reseau_polyline_Dir_Two AS SELECT ST_Reverse(srp.the_geom) as the_geom,id_tronc,from_node,to_node,speed_max,speed_load,queue.sd_cross as speed_cross,queue_len*1000 as queue_length FROM Simplified_Reseau_polyline as srp,"+dayt+"queue as queue WHERE srp.ORI=queue.to_node AND srp.EXT=queue.from_node;\n";
        sqlCode+="CREATE TABLE Queue_Reseau_polyline_WithoutTraffic AS select * from Queue_Reseau_polyline_Dir_One UNION select * from  Queue_Reseau_polyline_Dir_Two;\n";
        sqlCode+="drop table Queue_Reseau_polyline_Dir_One purge;\n";
        sqlCode+="drop table Queue_Reseau_polyline_Dir_Two purge;\n";
        // Append traffic information light & heavy vehicle
        sqlCode+="CREATE TABLE Queue_Reseau_polyline AS SELECT poly.*,tron.tv,tron.pl,tron.rtype FROM Queue_Reseau_polyline_WithoutTraffic as poly,"+dayt+"tron as tron WHERE poly.from_node=tron.from_node AND poly.to_node=tron.to_node;\n";
        sqlCode+="drop table Queue_Reseau_polyline_WithoutTraffic purge;\n";
        // Segments corresponding to waiting queue will be splitted from the original roads
        sqlCode+="CREATE TABLE Splited_Queue_Reseau_polyline as SELECT ST_SplitSegment(the_geom,queue_length) as the_geom,id_tronc,speed_max,speed_load,speed_cross,queue_length,ST_Length(the_geom) as original_length,tv,pl,rtype  FROM Queue_Reseau_polyline;\n";
        sqlCode+="drop table Queue_Reseau_polyline purge;\n";
        // Convert MULTILINESTRING to LINESTRING and add column is_queue
        sqlCode+="CREATE TABLE Kind_Reseau_polyline AS SELECT the_geom, id_tronc,((queue_length>=original_length) OR (queue_length>0.000001 AND explod_id=2)) as is_queue,speed_max,speed_load,speed_cross,tv,pl,rtype FROM ST_Explode(Splited_Queue_Reseau_polyline,'the_geom');\n";
        sqlCode+="drop table Splited_Queue_Reseau_polyline purge;\n"; 
                // Split LINESTRING into set of sub-segment (to keep only simple segment Z0 Z1)
        sqlCode+="CREATE TABLE multiseg AS SELECT ST_ToMultiSegments(the_geom) as the_geom, id_tronc,speed_max,speed_load,speed_cross,is_queue,tv,pl,rtype FROM Kind_Reseau_polyline;\n";
        sqlCode+="drop table Kind_Reseau_polyline purge;\n";
        sqlCode+="CREATE TABLE SplittedTronc AS SELECT the_geom,speed_max,speed_load,speed_cross,is_queue,tv,pl,rtype FROM  ST_Explode(multiseg,'the_geom');\n";
        sqlCode+="drop table multiseg purge;\n";
        sqlCode+="CREATE TABLE SegmentPointsWithLength AS SELECT the_geom,ST_ToMultiPoint(the_geom) as the_mpgeom, ST_Length(the_geom) as len,speed_max,speed_load,speed_cross,is_queue,tv,pl,rtype  FROM SplittedTronc WHERE ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),0))!=-99 and ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),1))!=-99;\n";
        sqlCode+="drop table SplittedTronc purge;\n";
        sqlCode+="CREATE TABLE traffic_with_z AS SELECT the_geom,len,ST_Z(ST_GeometryN(the_mpgeom,0)) as z0,ST_Z(ST_GeometryN(the_mpgeom,1)) as z1,speed_max,speed_load,speed_cross,is_queue,tv,pl,rtype FROM SegmentPointsWithLength;\n";
        sqlCode+="drop table SegmentPointsWithLength purge;\n";
        // Replace unsuported road type
        sqlCode+="UPDATE traffic_with_z SET rtype=56 WHERE rtype=1 OR rtype=71;\n";
        // Compute the sound level for each segment of roads
        sqlCode+="CREATE TABLE "+dayt+"main_dbm_global AS SELECT the_geom,BR_EvalSource(speed_load,tv-pl,pl,speed_cross,speed_max,rtype,z0,z1,len,is_queue) as db_m from traffic_with_z HAVING db_m > 0;\n";
        sqlCode+="drop table traffic_with_z purge;\n";
    }
}

for(int id=0;id<daytimes.length;id++) {
    String dayt=daytimes[id];
    if(computeSecondaryRoads) {
        //-----------------------------------------------------
        //-----------------------------------------------------
        //---------  BUILD SECONDARY ROADS SEGMENTS -----------
        // Set traffic information on secondary_roads
        sqlCode+="create table flux_with_road_count as select fz.num_zone,vl,pl,speed,roadcpt FROM "+dayt+"flux_zones as fz,area_roads_count as rc WHERE rc.num_zone=fz.num_zone;\n";
        sqlCode+="create table secondary_roads_with_traffic as select Pk(AutoNumeric()) as id_road,the_geom,rc.num_zone,(vl/roadcpt) as vl,(pl/roadcpt) as pl,speed FROM secondary_roads_with_areaid,flux_with_road_count rc WHERE rc.num_zone=secondary_roads_with_areaid.NUMERO;\n";
        sqlCode+="drop table flux_with_road_count purge;\n";
        // Split roads into segments
        sqlCode+="CREATE TABLE multiseg AS SELECT ST_ToMultiSegments(the_geom) as the_geom, id_road,the_geom,num_zone,vl,pl,speed FROM secondary_roads_with_traffic;\n";
        sqlCode+="DROP TABLE secondary_roads_with_traffic purge;\n";
        sqlCode+="CREATE TABLE exp_multiseg AS SELECT * FROM ST_Explode(multiseg);\n";
        sqlCode+="drop table multiseg purge;\n";
        sqlCode+="CREATE TABLE SplittedSecondaryRoads AS SELECT Pk(AutoNumeric()) as id_secondary_split,id_road, the_geom,ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),0)) as z0,ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),1)) as z1, ST_Length(the_geom) as len,num_zone,vl,pl,speed FROM exp_multiseg;\n";
        sqlCode+="drop table exp_multiseg purge;\n";
        // remove main roads then evaluate sound level on remaining roads
        sqlCode+="create table "+dayt+"secondary_roads_dbm_global AS SELECT id_secondary_split as ID,the_geom,BR_EvalSource(speed,vl,pl,speed,speed,31,z0,z1,len) as db_m FROM ST_SetNearestGeometryId(SplittedSecondaryRoads,Reseau_polyline,'the_geom','the_geom','NUM') where ID2=-1 OR avgDist>4 having db_m > 0;\n";
        sqlCode+="drop table SplittedSecondaryRoads purge;\n";
    }
}
for(int id=0;id<public_daytimes.length;id++) {
    String dayt=public_daytimes[id];
    //-----------------------------------------------------
    //-----------------------------------------------------
    // BUILD THIRD Source type BUS, BUSWAY & TRAMWAYS-------
    if(computeTramway) {
        // Tramway
        // Merge speed data with traffic data
        sqlCode+="create table tramway_data as SELECT sp.IDTRONC,sp.ORIG,sp.DEST,C_SP_TRAM AS SPEED_TRAM,traf.tramway FROM "+dayt+"bustram as traf,bustram_speed as sp WHERE sp.ROAD_TYPE=81 AND sp.IDTRONC = traf.id_tronc;\n";
        // Rigid ground(1), without anti-vibration systems(0)
        sqlCode+="create table tramway_spl as select IDTRONC,ORIG,DEST,BTW_EVALSOURCE(SPEED_TRAM,tramway,1,FALSE) as db_m from tramway_data;\n";
        sqlCode+="drop table tramway_data purge;\n";
        // Link with geometry
        sqlCode+="create table "+dayt+"tram_dbm_global as SELECT IDTRONC as ID,the_geom,db_m from tramway_spl,reseau_TW_BW_polyline as netw WHERE netw.NUM=tramway_spl.IDTRONC;\n";
        sqlCode+="drop table tramway_spl purge;\n";
    }
}
for(int id=0;id<public_daytimes.length;id++) {
    String dayt=public_daytimes[id];
    if(computeBusWay) {
        // BusWay
        sqlCode+="create table busway_data as SELECT sp.IDTRONC,sp.ORIG,sp.DEST,C_SP_BW AS SPEED_BW,traf.busway FROM "+dayt+"bustram as traf,bustram_speed as sp WHERE busway>0 AND sp.IDTRONC = traf.id_tronc;\n";
        sqlCode+="create table busway_spl as select IDTRONC,ORIG,DEST,BR_EvalSource(SPEED_BW,0,busway) as db_m from busway_data WHERE SPEED_BW>0;\n";
        sqlCode+="drop table busway_data purge;\n";
        sqlCode+="create table "+dayt+"busway_dbm_global AS SELECT IDTRONC as ID,the_geom,db_m from busway_spl,reseau_TW_BW_polyline as netw WHERE netw.NUM=busway_spl.IDTRONC;\n";
        sqlCode+="drop table busway_spl purge;\n";
    }
}
for(int id=0;id<public_daytimes.length;id++) {
    String dayt=public_daytimes[id];
    if(computeBus) {
        // Bus, take simplified_reseau_polyline of 2008 data (public transport only avaible for 2008)
        sqlCode+="create table bus_data as SELECT sp.IDTRONC,sp.ORIG,sp.DEST,C_SP_BUS AS SPEED_BUS,traf.bus,ROAD_TYPE FROM "+dayt+"bustram as traf,bustram_speed as sp WHERE bus>0 AND sp.IDTRONC = traf.id_tronc;\n";
        // Split LINESTRING into set of sub-segment (to keep only simple segment Z0 Z1)
        sqlCode+="CREATE TABLE multiseg AS SELECT ST_ToMultiSegments(the_geom) as the_geom,SPEED_BUS,bus,ROAD_TYPE  FROM Simplified_Reseau_polyline poly,bus_data bd WHERE (poly.ORI=bd.ORIG AND poly.EXT=bd.DEST) OR (poly.ORI=bd.DEST AND poly.EXT=bd.ORIG);\n";
        sqlCode+="drop table bus_data purge;\n";
        sqlCode+="CREATE TABLE exp_multiseg AS SELECT * FROM ST_Explode(multiseg,'the_geom');\n";
        sqlCode+="drop table multiseg purge;\n";
        sqlCode+="CREATE TABLE SegmentPointsWithLength AS SELECT the_geom,ST_ToMultiPoint(the_geom) as the_mpgeom, ST_Length(the_geom) as len,SPEED_BUS,bus,ROAD_TYPE  FROM exp_multiseg WHERE ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),0))!=-99 and ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),1))!=-99;\n";
        sqlCode+="drop table exp_multiseg purge;\n";
        sqlCode+="CREATE TABLE traffic_with_z AS SELECT the_geom,len,ST_Z(ST_GeometryN(the_mpgeom,0)) as z0,ST_Z(ST_GeometryN(the_mpgeom,1)) as z1,SPEED_BUS,bus,ROAD_TYPE FROM SegmentPointsWithLength;\n";
        sqlCode+="drop table SegmentPointsWithLength purge;\n";
        // Replace unsuported road type
        sqlCode+="UPDATE traffic_with_z SET ROAD_TYPE=56 WHERE ROAD_TYPE=1 OR ROAD_TYPE=71 OR ROAD_TYPE=72;\n";
        // Compute the sound level for each segment of roads
        sqlCode+="CREATE TABLE "+dayt+"bus_dbm_global AS SELECT Pk(AutoNumeric()) as ID,the_geom,BR_EvalSource(SPEED_BUS,0,bus,SPEED_BUS,SPEED_BUS,ROAD_TYPE,z0,z1,len,FALSE) as db_m from traffic_with_z having db_m > 0;\n";
        sqlCode+="UPDATE "+dayt+"bus_dbm_global SET db_m=0 WHERE db_m < 0 OR db_m > 200;\n";
        sqlCode+="drop table traffic_with_z purge;\n";
    }
}
sqlCode+="drop table if exists Simplified_Reseau_polyline purge;\n";
sqlCode+="drop table if exists secondary_roads_with_areaid purge;\n";
sqlCode+="drop table if exists area_roads_count purge;\n";
// The global value of each time period is computed
// Recomposition in Ld Le Ln (private road noise)

// Union of Main, Secondary Roads
if( computeLDEN ) {
    //-----------------------------------------
    //---- Time range recomposition for Public Transport
    
    for(int id=0;id<mode_transport.length;id++) {
        String transp=mode_transport[id];
        
        if(transp=="private") {
            //Merge primary and secondary roads for private roads
            for(int id=0;id<daytimes.length;id++) {
                String dayt=daytimes[id];
                if( computeSecondaryRoads && computeMainRoads ) {
                    sqlCode+="CREATE TABLE "+dayt+transp+"_dbm_global as select the_geom,db_m from "+dayt+"main_dbm_global UNION select the_geom,db_m from "+dayt+"secondary_roads_dbm_global;\n";
                } else if(computeMainRoads){
                    sqlCode+="CREATE TABLE "+dayt+transp+"_dbm_global as select the_geom,db_m from "+dayt+"main_dbm_global;\n";
                }
            }            
        }
        //Compute LE level, public transport do not have the same time ranges, then the sql request is different
        if((transp=="bus" && computeBus) || (transp=="busway" && computeBusWay) || (transp=="tram" && computeTramway)) {
            sqlCode+="create table LE_HCN as SELECT the_geom,10*(Log10((2/4)*Power(10,db_m/10))) as db_m FROM hcn_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LE_HCJ as SELECT the_geom,10*(Log10((1/4)*Power(10,db_m/10))) as db_m FROM hcj_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LE_HPS as SELECT the_geom,10*(Log10((1/4)*Power(10,db_m/10))) as db_m FROM hps_"+transp+"_dbm_global where db_m>0;\n";

            sqlCode+="create table LE_"+transp+" as  select * from LE_HCN UNION select * from LE_HCJ UNION select * from LE_HPS;\n";
            sqlCode+="drop table LE_HCJ purge;\n";
            sqlCode+="drop table LE_HPS purge;\n";
            sqlCode+="drop table LE_HCN purge;\n";

            sqlCode+="create table LN_HCN as SELECT the_geom,10*(Log10((5/10)*Power(10,db_m/10))) as db_m FROM hcn_"+transp+"_dbm_global ;\n";
            sqlCode+="create table LN_HCN2 as SELECT the_geom,10*(Log10((2/10)*Power(10,db_m/10))) as db_m FROM hcn2_"+transp+"_dbm_global ;\n";

            sqlCode+="create table LN_"+transp+" AS select * from LN_HCN UNION select * from LN_HCN2;\n";
            sqlCode+="drop table LN_HCN purge;\n";
            sqlCode+="drop table LN_HCN2 purge;\n";
        } else if(transp=="private" && (computeSecondaryRoads || computeMainRoads)) {
    // Compute LE for private vehicle
            sqlCode+="create table LE_HCN as SELECT the_geom,10*(Log10((2/4)*Power(10,db_m/10))) as db_m FROM hcn_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LE_HCJ as SELECT the_geom,10*(Log10((1/4)*Power(10,db_m/10))) as db_m FROM hcj_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LE_HPS as SELECT the_geom,10*(Log10((1/4)*Power(10,db_m/10))) as db_m FROM hps_"+transp+"_dbm_global where db_m>0;\n";

            sqlCode+="create table LE_"+transp+" as  select * from LE_HCN UNION select * from LE_HCJ UNION select * from LE_HPS;\n";
            sqlCode+="drop table LE_HCJ purge;\n";
            sqlCode+="drop table LE_HPS purge;\n";
            sqlCode+="drop table LE_HCN purge;\n";     
            sqlCode+="create table LN_"+transp+" AS select * from hcn_private_dbm_global;\n";       
        }
        
        
        if((transp=="bus" && computeBus) || (transp=="busway" && computeBusWay) || (transp=="tram" && computeTramway) || transp=="private" && (computeSecondaryRoads || computeMainRoads)) {
            // Compute LD same for all transport
            sqlCode+="create table LD_HPM as SELECT the_geom,10*(Log10((2/12)*Power(10,db_m/10))) as db_m FROM hpm_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LD_HCJ as SELECT the_geom,10*(Log10((9/12)*Power(10,db_m/10))) as db_m FROM hcj_"+transp+"_dbm_global where db_m>0;\n";
            sqlCode+="create table LD_HPS as SELECT the_geom,10*(Log10((1/12)*Power(10,db_m/10))) as db_m FROM hps_"+transp+"_dbm_global where db_m>0;\n";

            sqlCode+="create table LD_"+transp+" as select * from LD_HPM UNION select * from LD_HCJ UNION select * from LD_HPS;\n";
            sqlCode+="drop table LD_HPM purge;\n";
            sqlCode+="drop table LD_HCJ purge;\n";
            sqlCode+="drop table LD_HPS purge;\n";
     
            //Mix LD LE and LN for Lden
            sqlCode+="create table LDEN_LD as SELECT the_geom,10*(Log10((12/24)*Power(10,db_m/10))) as db_m FROM LD_"+transp+" where db_m>0;\n";
            sqlCode+="create table LDEN_LE as SELECT the_geom,10*(Log10((4/24)*Power(10,(db_m+5)/10))) as db_m FROM LE_"+transp+"  where db_m>0;\n";
            sqlCode+="create table LDEN_HCN as SELECT the_geom,10*(Log10((8/24)*Power(10,(db_m+10)/10))) as db_m FROM LN_"+transp+" where db_m>0;\n";


            sqlCode+="create table LDEN_"+transp+" as select * from LDEN_LD UNION select * from LDEN_LE UNION select * from LDEN_HCN;\n";
             
            sqlCode+="drop table LDEN_LD purge;\n";
            sqlCode+="drop table LDEN_LE purge;\n";
            sqlCode+="drop table LDEN_HCN purge;\n";
            
            //Compute final Lnight
            sqlCode+="create table Lnight_"+transp+" as select the_geom,db_m+10 as db_m from LN_"+transp+" where db_m>0;\n";
        }

    }   
}


//Export to file
FileWriter fstream = new FileWriter("/home/fortin/sqlout.sql");
BufferedWriter out = new BufferedWriter(fstream);
out.write(sqlCode);
//Close the output stream
out.close();
//SQL(sqlCode);







