package lcpc_son;

import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;

public class LcpcSonExtension extends Extension{

	@Override
	public void configure(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addRegisterFunction(BR_EvalSourceV1.class);
		context.getFeatureInstaller().addRegisterFunction(ST_SplitLineInPoints.class);
		context.getFeatureInstaller().addRegisterFunction(Log10.class);
	}
}
