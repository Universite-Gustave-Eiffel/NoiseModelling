# -*- coding: cp1252 -*-
from xls_to_dbf import ExtractXlsData,SetDefaultValueWhereNone,SaveArrayToDBF
import time
import sys,os
##
# Open XLS file and extract columns, then save to a database file in the dbf format.
# @param[in] saveto Path of the DBF file
# @param[in] day_time_to_extract morning,day,evening,night,average
def Convert(xlspath,saveto,day_time_to_extract):
    start_t=time.time()
    #day_time_to_extract= #morning,day,evening,night,average
    sheet_id=0
    start_line=3
    field_line=2
    base_cols=[0]
    morningcol=16
    daycol=19
    eveningcol=22
    nightcol=25
    avgcol=28
    speed_column=31
    cols={"morning" : [morningcol,morningcol+1,speed_column],
          "day"     : [daycol,daycol+1,speed_column],
          "evening" : [eveningcol,eveningcol+1,speed_column],
          "night"   : [nightcol,nightcol+1,speed_column],
          "average"  : [avgcol,avgcol+1,speed_column]}
    col_label=["num_zone","vl","pl","speed"]
    fields,data=ExtractXlsData(xlspath,sheet_id,start_line,base_cols+cols[day_time_to_extract],field_line) #+cols[day_time_to_extract]
    data=data[:-1] #remove the sum
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save to dbf file"
    SaveArrayToDBF(saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)
time_ranges=["morning" , "day" , "evening","night","average"]
time_ranges_lbl=["hpm_","hcj_","hps_","hcn_","avg_"]
choice=0
print "Choose day range to extract 0-%i :" % (len(time_ranges))
print ["%i:%s" % (id,name) for id,name in enumerate(time_ranges)]
choice=int(raw_input('Time range id:'))
if not os.path.exists(sys.argv[1]):
    raise(ValueError("Input file does not exists :\n"+sys.argv[1]))
Convert(sys.argv[1],time_ranges_lbl[choice]+"flux_zones.dbf",time_ranges[choice])

