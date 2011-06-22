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
    start_line=1
    field_line=0
    base_cols=[1]
    col_label=["id_tronc"]
    fields,data=ExtractXlsData("liste_troncons_ponts_100713.xls",sheet_id,start_line,base_cols,field_line)
    print u"Set default values in empty cells"
    data=SetDefaultValueWhereNone(data)
    print u"Save to dbf file"
    SaveArrayToDBF(saveto,col_label,data)
    print u"Extraction of data done in %g seconds." % (time.time()-start_t)
Convert("tron_bridge.dbf")
raw_input()
