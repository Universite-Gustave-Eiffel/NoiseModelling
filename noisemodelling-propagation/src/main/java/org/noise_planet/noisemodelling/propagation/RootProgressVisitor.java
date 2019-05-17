package org.noise_planet.noisemodelling.propagation;

import org.h2gis.api.ProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RootProgressVisitor extends DefaultProgressVisitor {
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private boolean canceled = false;
    private boolean logProgression = false;
    private int progressionLogStep = 1;
    private Logger logger = LoggerFactory.getLogger(RootProgressVisitor.class);
    private int lastLoggedProgression = Integer.MIN_VALUE;

    public RootProgressVisitor(long subprocessSize) {
        super(subprocessSize, null);
    }


    public RootProgressVisitor(long subprocessSize, boolean logProgression, int progressionLogStep) {
        super(subprocessSize, null);
        this.logProgression = logProgression;
        this.progressionLogStep = progressionLogStep;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property, listener);
    }

    @Override
    protected synchronized void pushProgression(double incProg) {
        double oldProgress = getProgression();
        super.pushProgression(incProg);
        double newProgress = getProgression();
        propertyChangeSupport.firePropertyChange("PROGRESS", oldProgress, newProgress);
        if(logProgression) {
            int newLogProgress = Double.valueOf(newProgress * 100).intValue();
            if(newLogProgress - lastLoggedProgression >= progressionLogStep) {
                lastLoggedProgression = newLogProgress;
                logger.info(String.format("%d %%", newLogProgress));
            }
        }
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void cancel() {
        canceled = true;
        propertyChangeSupport.firePropertyChange(ProgressVisitor.PROPERTY_CANCELED, false, true);
    }
}
