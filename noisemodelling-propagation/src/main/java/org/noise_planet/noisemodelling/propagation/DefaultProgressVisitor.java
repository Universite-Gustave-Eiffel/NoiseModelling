package org.noise_planet.noisemodelling.propagation;

import org.h2gis.api.ProgressVisitor;

import java.beans.PropertyChangeListener;

public class DefaultProgressVisitor implements ProgressVisitor {
    protected long subprocessSize;
    protected double subprocessDone = 0;
    DefaultProgressVisitor parentProcess;

    public DefaultProgressVisitor(long subprocessSize, DefaultProgressVisitor parentProcess) {
        this.subprocessSize = subprocessSize;
        this.parentProcess = parentProcess;
    }

    @Override
    public ProgressVisitor subProcess(int i) {
        return new DefaultProgressVisitor(i, this);
    }

    @Override
    public void endStep() {
        pushProgression(1.0);
    }

    protected synchronized void pushProgression(double incProg) {
        if (subprocessDone + incProg <= subprocessSize) {
            subprocessDone += incProg;
            if (parentProcess != null) {
                parentProcess.pushProgression((incProg / subprocessSize));
            }
        }
    }

    @Override
    public void setStep(int i) {
    }

    @Override
    public int getStepCount() {
        return (int)subprocessSize;
    }

    @Override
    public void endOfProgress() {
    }

    @Override
    public double getProgression() {
        if(parentProcess != null) {
            return parentProcess.getProgression();
        } else {
            return subprocessDone / subprocessSize;
        }
    }

    @Override
    public boolean isCanceled() {
        return parentProcess != null && parentProcess.isCanceled();
    }

    @Override
    public void cancel() {
        if(parentProcess != null) {
            parentProcess.cancel();
        }
    }

    @Override
    public void addPropertyChangeListener(String s, PropertyChangeListener propertyChangeListener) {
        if(parentProcess != null) {
            parentProcess.addPropertyChangeListener(s, propertyChangeListener);
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        if(parentProcess != null) {
            parentProcess.removePropertyChangeListener(propertyChangeListener);
        }
    }
}
