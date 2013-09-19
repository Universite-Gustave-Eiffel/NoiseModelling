/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 *
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.noisemap.plugin;

import java.util.Dictionary;
import java.util.Hashtable;
import org.gdms.sql.function.Function;
import org.noisemap.core.BR_EvalSource;
import org.noisemap.core.BR_PtGrid;
import org.noisemap.core.BR_SpectrumRepartition;
import org.noisemap.core.BR_TriGrid;
import org.noisemap.core.BTW_EvalSource;
import org.noisemap.core.BTW_SpectrumRepartition;
import org.noisemap.core.Log10;
import org.noisemap.core.ST_ExtractVerticesTriGrid;
import org.noisemap.core.ST_SetNearestGeometryId;
import org.noisemap.core.ST_SetNearestZ;
import org.noisemap.core.ST_SplitLineInPoints;
import org.noisemap.core.ST_SplitSegment;
import org.noisemap.core.ST_TableGeometryUnion;
import org.noisemap.core.ST_TriangleContouring;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Declare SQL functions
 * @author Nicolas Fortin
 */
public class Activator implements BundleActivator {
    private BundleContext context;
    @Override
    public void start(BundleContext context) {
        this.context = context;
        reg(new BR_EvalSource());
        reg(new ST_SplitLineInPoints());
        reg(new Log10());
        reg(new ST_SetNearestZ());
        reg(new ST_SetNearestGeometryId());
        reg(new ST_SplitSegment());
        reg(new BR_TriGrid());
  //      reg(new ST_TriangleContouring());
        reg(new BR_SpectrumRepartition());
        reg(new BTW_SpectrumRepartition());
        reg(new BTW_EvalSource());
        reg(new ST_TableGeometryUnion());
        reg(new ST_ExtractVerticesTriGrid());
        reg(new BR_PtGrid());
    }

    private void reg(Function gdmsFunc) {
        // Dict for visual hint for service list
        // inspect service capability #id
        Dictionary<String,String> prop = new Hashtable<String, String>();
        prop.put("name",gdmsFunc.getName());
        context.registerService(Function.class,
                gdmsFunc,
                prop);
    }
    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
