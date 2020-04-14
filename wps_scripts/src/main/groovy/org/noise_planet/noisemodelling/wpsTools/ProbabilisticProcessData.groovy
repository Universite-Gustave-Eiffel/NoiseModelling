package org.noise_planet.noisemodelling.wpsTools

import groovy.sql.Sql
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RSParametersDynamic

import java.sql.SQLException

/**
 *
 */
class ProbabilisticProcessData {

   Map<Integer, Double> SPEED_LV = new HashMap<>()
   Map<Integer, Double> SPEED_HV = new HashMap<>()
   Map<Integer, Double> LV = new HashMap<>()
   Map<Integer, Double> HV = new HashMap<>()

   double[] getCarsLevel(int idSource) throws SQLException {
       double[] res_d = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
       double[] res_LV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
       double[] res_HV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
       def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
       // memes valeurs d e et n


       def random = Math.random()
       if (random < LV.get(idSource)) {
           int kk = 0
           for (f in list) {

               double speed = SPEED_LV.get(idSource)
               int acc = 0
               int FreqParam = f
               double Temperature = 20
               int RoadSurface = 0
               boolean Stud = true
               double Junc_dist = 200
               int Junc_type = 1
               int veh_type = 1
               int acc_type = 1
               double LwStd = 1
               int VehId = 10

               RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
               rsParameters.setSlopePercentage(0)

               res_LV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
               kk++
           }

       }
       if (random < HV.get(idSource)) {
           int kk = 0
           for (f in list) {
               double speed = SPEED_HV.get(idSource)
               int acc = 0
               int FreqParam = f
               double Temperature = 20
               int RoadSurface = 0
               boolean Stud = true
               double Junc_dist = 200
               int Junc_type = 1
               int veh_type = 3
               int acc_type = 1
               double LwStd = 1
               int VehId = 10

               RSParametersDynamic rsParameters = new RSParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
               rsParameters.setSlopePercentage(0)

               res_HV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
               kk++
           }
       }
       int kk = 0
       for (f in list) {
           res_d[kk] = 10 * Math.log10(
                   (1.0 / 2.0) *
                           (Math.pow(10, (10 * Math.log10(Math.pow(10, res_LV[kk] / 10))) / 10)
                                   + Math.pow(10, (10 * Math.log10(Math.pow(10, res_HV[kk] / 10))) / 10)
                           )
           )
           kk++
       }


       return res_d
   }

   void setProbaTable(String tablename, Sql sql) {
       //////////////////////
       // Import file text
       //////////////////////
       int i_read = 0;

       // Remplissage des variables avec le contenu du fichier plan d'exp
       sql.eachRow('SELECT PK,  SPEED, HV,LV FROM ' + tablename + ';') { row ->
           int pk = (int) row[0]

           SPEED_HV.put(pk, (double) row[1])
           SPEED_LV.put(pk, (double) row[1])
           HV.put(pk, (double) row[2])
           LV.put(pk, (double) row[3])

       }


   }

}