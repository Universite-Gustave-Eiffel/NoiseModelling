/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.plugin;



import org.orbisgis.core.ui.pluginSystem.AbstractPlugIn;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;
/**
 *
 * @author fortin
 */
public class NoiseMapDeclaration extends AbstractPlugIn  {
    NoiseMapExtension noiseExt;
    @Override
    public void initialize(PlugInContext context) throws Exception {
        noiseExt=new NoiseMapExtension();
    }

    @Override
    public boolean execute(PlugInContext context) throws Exception {
        noiseExt.configure(context);
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
