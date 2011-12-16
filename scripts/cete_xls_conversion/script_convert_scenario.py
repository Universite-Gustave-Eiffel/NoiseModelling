# -*- coding: cp1252 -*-
from xls_to_dbf import ParseXlsData,FilterXlsData,SetDefaultValueWhereNone,SaveArrayToDBF
import time
import sys
import os

##
# Open XLS file and extract columns, then save to a database file in the dbf format.
# @param[in] saveto Path of the DBF file
# @param[in] day_time_to_extract morning,day,evening,night,average
def Convert(sheet_id,fulldata=None,xlsfilename=u"calcul vk par scenarios_valeurs.xls"):
    start_t=time.time()
    time_ranges=["morning" , "day" , "evening","night"]
    time_ranges_lbl=["hpm_","hcj_","hps_","hcn_"]
    if fulldata is None:
        print u"Extract fields from XLS file.."
        fulldata=ParseXlsData(xlsfilename)
        print u"Fields extraction done."
    for choice in range(len(time_ranges_lbl)):
        day_time_to_extract=time_ranges[choice]
        traffic_saveto=time_ranges_lbl[choice]+"tron.dbf"
        print u"Filter %s time range.." % (day_time_to_extract)
        start_line=2
        field_line=1
        base_cols=[1,2,3,5,6,7,9,12,13]
        morning_first_col=15
        day_first_col=19
        evening_first_col=23
        night_first_col=27
        cols={ "morning" : [morning_first_col,morning_first_col+1,morning_first_col+3],
         "day" : [day_first_col,day_first_col+1,day_first_col+3],
         "evening" : [evening_first_col,evening_first_col+1,evening_first_col+3],
         "night" : [night_first_col,night_first_col+1,night_first_col+3] }
        col_label=["id_tronc","from_node","to_node","direction","surtype","rtype","length","capacity","speedmax","tv","pl","speedload"]        
        fields,data=FilterXlsData(fulldata,sheet_id,start_line,base_cols+cols[day_time_to_extract],field_line)
        print u"Set default values in empty cells"
        data=SetDefaultValueWhereNone(data)
        print u"Save traffic to dbf file",traffic_saveto
        SaveArrayToDBF(traffic_saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)

choice=0

if len(sys.argv)<2:
    raise ValueError("This script request at least 1 argument. Xml filenames of roads traffic and sheet id")

Convert(sheet_id=int(sys.argv[2]),xlsfilename=sys.argv[1])