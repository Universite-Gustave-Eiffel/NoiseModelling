#
# NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
# evaluate the noise impact on urban mobility plans. This model is
# based on the French standard method NMPB2008. It includes traffic-to-noise
# sources evaluation and sound propagation processing.
#
# This version is developed at French IRSTV Institute and at IFSTTAR
# (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
# French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
#
# Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
# Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
# as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
#
# Copyright (C) 2011 IFSTTAR
# Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
#
# Noisemap is free software: you can redistribute it and/or modify it under the
# terms of the GNU General Public License as published by the Free Software
# Foundation, either version 3 of the License, or (at your option) any later
# version.
#
# Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
# A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# Noisemap. If not, see <http://www.gnu.org/licenses/>.
#
# For more information, please consult: <http://www.orbisgis.org/>
# or contact directly:
# info_at_ orbisgis.org
#

# -*- coding: cp1252 -*-
from xls_to_dbf import ParseXlsData,FilterXlsData,SetDefaultValueWhereNone,SaveArrayToDBF
import time
import sys
import os
def extract_queue(xlsdata,day_time_to_extract,queue_saveto):
    sheet_id=2
    start_line=2
    field_line=1
    base_cols=[1,2,3,7]
    morning_first_col=8
    day_first_col=11
    evening_first_col=14
    night_first_col=17
    morning_second_col=20
    day_second_col=23
    evening_second_col=26
    night_second_col=29
    cols={ "morning" : [morning_first_col,morning_first_col+1,morning_first_col+2,morning_second_col,morning_second_col+1], "day" : [day_first_col,day_first_col+1,day_first_col+2,day_second_col,day_second_col+1], "evening" : [evening_first_col,evening_first_col+1,evening_first_col+2,evening_second_col,evening_second_col+1], "night" : [night_first_col,night_first_col+1,night_first_col+2,night_second_col,night_second_col+1]}
    if not day_time_to_extract in cols.keys():
        return
    col_label=["id_tronc","from_node","to_node","speed_max","speed_load","time_cross","sd_cross","sd_cross_i","queue_len"]
    fields,data=FilterXlsData(xlsdata,sheet_id,start_line,base_cols+cols[day_time_to_extract],field_line)
    data=SetDefaultValueWhereNone(data)
    SaveArrayToDBF(queue_saveto,col_label,data)
##
# Open XLS file and extract columns, then save to a database file in the dbf format.
# @param[in] saveto Path of the DBF file
# @param[in] day_time_to_extract morning,day,evening,night,average
def Convert(day_time_to_extract,traffic_saveto,queue_saveto,fulldata=None,xlsfilename=u"tron�ons_calage_2002_v2.xls"):
    start_t=time.time()
    #day_time_to_extract= #morning,day,evening,night,average
    sheet_id=1
    start_line=2
    field_line=1
    base_cols=[1,2,3,5,6,7,9,12,13]
    morning_first_col=15
    day_first_col=23
    evening_first_col=31
    night_first_col=39
    avg_first_col=47
    cols={ "morning" : [morning_first_col+2,morning_first_col+3,morning_first_col+5,morning_first_col+7], "day" : [day_first_col+2,day_first_col+3,day_first_col+5,day_first_col+7], "evening" : [evening_first_col+2,evening_first_col+3,evening_first_col+5,evening_first_col+7], "night" : [night_first_col+2,night_first_col+3,night_first_col+5,night_first_col+7], "average" : [avg_first_col+2,avg_first_col+3,avg_first_col+4,avg_first_col+4] }
    col_label=["id_tronc","from_node","to_node","direction","surtype","rtype","length","capacity","speedmax","tv","pl","speedload","speedcross"]
    if fulldata is None:
        print u"Extract %s fields from XLS file.." % (day_time_to_extract)
        fulldata=ParseXlsData(xlsfilename)
    fields,data=FilterXlsData(fulldata,sheet_id,start_line,base_cols+cols[day_time_to_extract],field_line)
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save traffic to dbf file",traffic_saveto
    SaveArrayToDBF(traffic_saveto,col_label,data)
    print u"Save queue to dbf file :",queue_saveto
    extract_queue(fulldata,day_time_to_extract,queue_saveto)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)
def ConvertBusTram(day_time_to_extract,traffic_saveto,fulldata=None,xlsfilename=u"Services TC 2008.xls"):
    start_t=time.time()
    #day_time_to_extract= #morning,day,evening,night,average
    sheet_id=0
    start_line=3
    field_line=2
    base_cols=[0]
    morning_first_col=1
    day_first_col=3
    evening_first_col=2
    night_first_col=4  #4: 20h-1h
    night2_first_col=5 #5:  4h-6h
    avg_first_col=6
    cols={ "morning" : [morning_first_col,morning_first_col+6,morning_first_col+12],
           "day" : [day_first_col,day_first_col+6,day_first_col+12],
           "evening" : [evening_first_col,evening_first_col+6,evening_first_col+12],
           "night" : [night_first_col,night_first_col+6,night_first_col+12],
           "night2" : [night2_first_col,night2_first_col+6,night2_first_col+12],
           "average" : [avg_first_col,avg_first_col+6,avg_first_col+12] }
    col_label=["id_tronc","bus","busway","tramway"]
    if fulldata is None:
        print u"Extract %s fields from XLS file.." % (day_time_to_extract)
        fulldata=ParseXlsData(xlsfilename)
    fields,data=FilterXlsData(fulldata,sheet_id,start_line,base_cols+cols[day_time_to_extract],field_line)
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save tramway and bus traffic to dbf file"
    SaveArrayToDBF(traffic_saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)

def ConvertBusTramSpeed(speed_saveto,fulldata=None,xlsfilename=u"tps tc troncons.xls"):
    start_t=time.time()
    #day_time_to_extract= #morning,day,evening,night,average
    sheet_id=0
    start_line=3
    field_line=2
    base_cols=[0,1,2,3,5,8,9,12,14,15,18]
    col_label=["IDTRONC","ORIG","DEST","ROAD_TYPE","KM_LENGTH","T_DUR_BUS","T_DUR_BW","T_DUR_TW","C_SP_BUS","C_SP_BW","C_SP_TRAM"]
    if fulldata is None:
        print u"Extract fields from XLS file.."
        fulldata=ParseXlsData(xlsfilename)
    fields,data=FilterXlsData(fulldata,sheet_id,start_line,base_cols,field_line)
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save tramway and bus traffic to dbf file"
    SaveArrayToDBF(speed_saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)

time_ranges=["morning" , "day" , "evening","night","night2","average"]
time_ranges_lbl=["hpm_","hcj_","hps_","hcn_","hcn2_","avg_"]
choice=0

if len(sys.argv)<3:
    raise ValueError("This script request at least 3 arguments. Xml filenames of roads traffic,tramway&busway traffic,tramway&busway speed")
if len(sys.argv)<5:    
    print "Choose day range to extract 0-%i :" % (len(time_ranges))
    print ["%i:%s" % (id,name) for id,name in enumerate(time_ranges)]
    choice=int(raw_input('Time range id:'))
else:
    choice=int(sys.argv[4])
if time_ranges[choice]!="night2" and os.path.exists(sys.argv[1]):
    Convert(time_ranges[choice],time_ranges_lbl[choice]+"tron.dbf",time_ranges_lbl[choice]+"queue.dbf",xlsfilename=sys.argv[1])
else:
    print "Skip tron and queue, file doesn't exist or are not specified"
if os.path.exists(sys.argv[2]):
    ConvertBusTram(time_ranges[choice],time_ranges_lbl[choice]+"bustram.dbf",xlsfilename=sys.argv[2])
else:
    print "Skip bustram.dbf, file doesn't exist or are not specified"
if os.path.exists(sys.argv[3]):
    ConvertBusTramSpeed("bustram_speed.dbf",xlsfilename=sys.argv[3])
else:
    print "Skip bustram_speed.dbf, file doesn't exist or are not specified"