/***********************************
 * ANR EvalPDU
 * IFSTTAR 08_12_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/
import org.orbisgis.core.ui.plugins.views.beanShellConsole.commands.SQL;
import java.io.*;

String sqlCode="";
String receiver_tablename="receiver_spl_lnight_private_public";
String sound_data_type="2002_lnight_private_public";
String path_export="/media/projects_/module_bruit/data_computed/cu_nantes_perval/perval_result_to_mail/2002_LDEN_TRAMWAY/";
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
    sqlCode+="create table join_table as select num_act_pe,perv.parc_id,max_db,min_db,avg_db as moy_db,dev_db as ecart_type from "+parc_type+"_perval_with_id perv LEFT JOIN PARCELLES_44_db db ON perv.parc_id = db.parc_id;\n";
    sqlCode+="create table dansparc_"+parc_type+"_"+sound_data_type+" as select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from join_table where max_db is not null UNION select num_act_pe,parc_id,LOG10(0) as max_db,LOG10(0) as min_db,LOG10(0) as moy_db,0 as ecart_type from join_table where max_db is null;\n";
    sqlCode+="drop table join_table purge;\n";
    sqlCode+="create table join_table as select num_act_pe,perv.parc_id,max_db,min_db,avg_db as moy_db,dev_db as ecart_type from "+parc_type+"_perval_with_id perv LEFT JOIN PARCELLES_44_db_buffered db ON perv.parc_id = db.parc_id;\n";
    sqlCode+="create table autourparc_"+parc_type+"_"+sound_data_type+" as select num_act_pe,parc_id,max_db,min_db,moy_db, ecart_type from join_table where max_db is not null UNION select num_act_pe,parc_id,LOG10(0) as max_db,LOG10(0) as min_db,LOG10(0) as moy_db,0 as ecart_type from join_table where max_db is null;\n";
    sqlCode+="EXECUTE Export('autourparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"autourparc_"+parc_type+"_"+sound_data_type+".csv');\n";
    sqlCode+="EXECUTE Export('dansparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"dansparc_"+parc_type+"_"+sound_data_type+".csv');\n";
    sqlCode+="EXECUTE Export('autourparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"autourparc_"+parc_type+"_"+sound_data_type+".dbf');\n";
    sqlCode+="EXECUTE Export('dansparc_"+parc_type+"_"+sound_data_type+"', '"+path_export+"dansparc_"+parc_type+"_"+sound_data_type+".dbf');\n";
    sqlCode+="drop table join_table purge;\n";
}

FileWriter fstream = new FileWriter("/home/fortin/sqlperval.sql");
BufferedWriter out = new BufferedWriter(fstream);
out.write(sqlCode);
out.close();
//SQL(sqlCode);