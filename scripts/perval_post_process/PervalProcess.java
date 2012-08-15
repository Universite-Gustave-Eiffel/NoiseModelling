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
String receiver_tablename="receivers_test5_lden_vl_pl_bus_busway_tram";
String sound_data_type="test5_lden_private_public";
String path_export="/home/fortin/computation/TEST5_LDEN_ROUTIER_FERROVIAIRE/";
String[] parc_types={"terrain","maison","appart"};

// First Step, found the nearest point of receivers related to perval area (10 meter max)
sqlCode+="create table parc44_dist as select * from ST_SetNearestGeometryId("+receiver_tablename+",  parcelles_perval_rowid , 'the_geom','the_geom', 'idrow',10);\n";
// Group and Filter results by the perval ID. For the receiver inside parc and within x meter buffer
sqlCode+="create table parc_db_inside as select ID2,MAX(db_m) as max_lvl,MIN(db_m) as min_lvl,AVG(db_m) as avg_lvl, StandardDeviation(10*log10(db_m)) as dev_lvl from parc44_dist where avgDist < 0.0001 and db_m>1 GROUP BY ID2;\n";
sqlCode+="create table parc_db_buffered as select ID2,MAX(db_m) as max_lvl,MIN(db_m) as min_lvl,AVG(db_m) as avg_lvl, StandardDeviation(10*log10(db_m)) as dev_lvl from parc44_dist where avgDist < 5 and db_m>1 GROUP BY ID2;\n";
// Retrieve parc_id from PARCELLES_44_rowid table
sqlCode+="create table PARCELLES_44_db as select the_geom,parc_id,10*LOG10(max_lvl) as max_db,10*LOG10(min_lvl) as min_db,10*LOG10(avg_lvl) as avg_db ,dev_lvl as dev_db from parcelles_perval_rowid perv, parc_db_inside parc where perv.idrow = parc.ID2;\n";
sqlCode+="create table PARCELLES_44_db_buffered as select the_geom,parc_id,10*LOG10(max_lvl) as max_db,10*LOG10(min_lvl) as min_db,10*LOG10(avg_lvl) as avg_db ,dev_lvl as dev_db from parcelles_perval_rowid perv, parc_db_buffered parc where perv.idrow = parc.ID2;\n";
sqlCode+="drop table parc_db_inside purge;\n";
sqlCode+="drop table parc_db_buffered purge;\n";
for(int id=0;id<parc_types.length;id++) {
    String parc_type=parc_types[id];
    // Création des tables finales
    sqlCode+="create table strict_join_table as select num_act_pe,perv.parc_id,max_db,min_db,avg_db as moy_db,dev_db as ecart_type from "+parc_type+"_perval_with_id perv, PARCELLES_44_db db WHERE perv.parc_id = db.parc_id;\n";
    sqlCode+="create table outer_join_table as select num_act_pe,parc_id,LOG10(0) as max_db,LOG10(0) as min_db,LOG10(0) as moy_db,0 as ecart_type from "+parc_type+"_perval_with_id perv WHERE num_act_pe NOT IN (select strict_join_table.num_act_pe from strict_join_table);\n";
    sqlCode+="create table dansparc_"+parc_type+"_"+sound_data_type+" as select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from strict_join_table UNION select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from outer_join_table;\n";
    sqlCode+="drop table strict_join_table purge;\n";
    sqlCode+="drop table outer_join_table purge;\n";
    sqlCode+="create table strict_join_table as select num_act_pe,perv.parc_id,max_db,min_db,avg_db as moy_db,dev_db as ecart_type from "+parc_type+"_perval_with_id perv, PARCELLES_44_db_buffered db WHERE perv.parc_id = db.parc_id;\n";
    sqlCode+="create table outer_join_table as select num_act_pe,parc_id,LOG10(0) as max_db,LOG10(0) as min_db,LOG10(0) as moy_db,0 as ecart_type from "+parc_type+"_perval_with_id perv WHERE num_act_pe NOT IN (select strict_join_table.num_act_pe from strict_join_table);\n";
    sqlCode+="create table autourparc_"+parc_type+"_"+sound_data_type+" as select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from strict_join_table UNION select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from outer_join_table;\n";
    sqlCode+="drop table strict_join_table purge;\n";
    sqlCode+="drop table outer_join_table purge;\n";
    sqlCode+="EXECUTE Export('autourparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"autourparc_"+parc_type+"_"+sound_data_type+".csv');\n";
    sqlCode+="EXECUTE Export('dansparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"dansparc_"+parc_type+"_"+sound_data_type+".csv');\n";
    sqlCode+="EXECUTE Export('autourparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"autourparc_"+parc_type+"_"+sound_data_type+".dbf');\n";
    sqlCode+="EXECUTE Export('dansparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"dansparc_"+parc_type+"_"+sound_data_type+".dbf');\n";

}

SQL(sqlCode);

