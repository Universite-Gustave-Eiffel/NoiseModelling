package org.orbisgis.noisemap.h2osgi;

import org.h2gis.api.Function;
import org.orbisgis.noisemap.h2.BR_EvalSource;
import org.orbisgis.noisemap.h2.BR_PtGrid;
import org.orbisgis.noisemap.h2.BR_PtGrid3D;
import org.orbisgis.noisemap.h2.BR_SpectrumRepartition;
import org.orbisgis.noisemap.h2.BR_TriGrid;
import org.orbisgis.noisemap.h2.BR_TriGrid3D;
import org.orbisgis.noisemap.h2.BTW_EvalSource;
import org.orbisgis.noisemap.h2.BTW_SpectrumRepartition;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * As H2 function need to be in fragment bundle. This activator need to be placed in another package.
 * @author Nicolas Fortin
 */
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext ctx) throws Exception {
        reg(ctx, new BR_PtGrid());
        reg(ctx, new BR_PtGrid3D());
        reg(ctx, new BR_EvalSource());
        reg(ctx, new BR_SpectrumRepartition());
        reg(ctx, new BR_TriGrid());
        reg(ctx, new BR_TriGrid3D());
        reg(ctx, new BTW_EvalSource());
        reg(ctx, new BTW_SpectrumRepartition());
    }

    private void reg(BundleContext ctx, Function func) {
        ctx.registerService(Function.class, func, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
