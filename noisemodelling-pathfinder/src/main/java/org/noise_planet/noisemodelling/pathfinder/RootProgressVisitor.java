package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.api.ProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RootProgressVisitor extends DefaultProgressVisitor {
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private boolean canceled = false;
    private boolean logProgression = false;
    private Logger logger = LoggerFactory.getLogger(RootProgressVisitor.class);
    private String lastLoggedProgression = "";
    private double minimumSecondsBetweenPrint = 1.0;
    private long lastPrint = 0;

    public RootProgressVisitor(long subprocessSize) {
        super(subprocessSize, null);
    }


    public RootProgressVisitor(long subprocessSize, boolean logProgression, double minimumSecondsBetweenPrint) {
        super(subprocessSize, null);
        this.logProgression = logProgression;
        this.minimumSecondsBetweenPrint = minimumSecondsBetweenPrint;
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
            String newLogProgress = String.format("CUSTOMMESSAGE %.2f %%", newProgress * 100);
            if(!newLogProgress.equals(lastLoggedProgression)) {
                lastLoggedProgression = newLogProgress;
                long t = System.currentTimeMillis();
                if((t - lastPrint) / 1000.0 > minimumSecondsBetweenPrint) {
                    logger.info(newLogProgress);
                    lastPrint = t;
                }
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
