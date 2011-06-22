# -*- coding: cp1252 -*-
from xls_to_dbf import ExtractXlsData,SetDefaultValueWhereNone,SaveArrayToDBF
import time
##
# Open XLS file and extract columns, then save to a database file in the dbf format.
# @param[in] saveto Path of the DBF file
# @param[in] day_time_to_extract morning,day,evening,night,average
def Convert(saveto):
    start_t=time.time()
    #day_time_to_extract= #morning,day,evening,night,average
    sheet_id=0
    start_line=10
    field_line=9
    base_cols=range(7)
    col_label=["num_zone","avg_dist","stream","modal","vl","vl_km","speed"]
    fields,data=ExtractXlsData("flux_intrazonaux_2002_v2.xls",sheet_id,start_line,base_cols,field_line)
    data=data[:-1] #remove the sum
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save to dbf file"
    SaveArrayToDBF(saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)
Convert("flux_zones.dbf")
raw_input()
