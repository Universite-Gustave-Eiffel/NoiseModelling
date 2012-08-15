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

