package org.noise_planet.noisemodelling.jdbc;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;
import org.noise_planet.noisemodelling.emission.RailWayLW;

import java.util.Locale;

public class PolarGraphDirectivity {
    double dwidth = 500;
    double dheight = 500;
    double radius = dwidth/2 - 80;
    double textRadius = radius + 25;
    double centerx = dwidth/2;
    double centery = dheight/2;

    public enum ORIENTATION {TOP, FRONT, SIDE}

    private void generateLine(StringBuilder sb, double startX, double startY, double stopX, double stopY, String color) {
        sb.append(String.format(Locale.ROOT, "<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\"" +
                " stroke=\"%s\" stroke-width=\"1\" />\n",startX, startY, stopX, stopY, color));
    }

    private void generateDashedLine(StringBuilder sb, double startX, double startY, double stopX, double stopY, String color) {
        sb.append(String.format(Locale.ROOT, "<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\"" +
                " stroke=\"%s\" stroke-width=\"1\" stroke-dashoffset=\"5\"  stroke-dasharray=\"5,5\" />\n",startX, startY, stopX, stopY, color));
    }

    private void generateText(StringBuilder sb, double startX, double startY, int fontSize, String text, String verticalAlignement) {
        sb.append(String.format(Locale.ROOT, "<text x=\"%f\" y=\"%f\" font-family=\"Verdana\" font-size=\"%d\"  text-anchor=\"middle\" dominant-baseline=\"%s\">%s</text>\n",startX, startY, fontSize, verticalAlignement,text));
    }

    private double toRadian(double angle) {
        return (angle / 180.0) * Math.PI;
    }

    private int getAdjustedAngle(int angle, ORIENTATION orientation) {
        if(orientation == ORIENTATION.TOP) {
            return (angle + 90 ) % 360; // return (630 - angle) % 360;
        } else {
            return (720 - angle) % 360;
        }
    }

    private void generateLegend(StringBuilder sb, double value, double position, double angle) {
        double destX = centerx + Math.cos(toRadian(angle)) * radius * position;
        double destY = centery + Math.sin(toRadian(angle)) * radius * position;
        generateText(sb, destX, destY,10,String.format(Locale.ROOT,
                "%.0f dB", value), "middle");
    }

    public String generatePolarGraph(RailWayLW.TrainNoiseSource noiseSource, double frequency, double minimumAttenuation, double maximumAttenuation, ORIENTATION orientation) {

        // HEADER
        StringBuilder sb = new StringBuilder(String.format("<svg height=\"%d\" width=\"%d\">\n", (int)dheight, (int)dwidth));
        // CIRCLES
        sb.append(String.format(Locale.ROOT, "<circle cx=\"%f\" cy=\"%f\" r=\"%f\" stroke=\"grey\" " +
                "stroke-width=\"1\"  fill=\"transparent\"/>\n",centerx, centery, radius));
        sb.append(String.format(Locale.ROOT, "<circle cx=\"%f\" cy=\"%f\" r=\"%f\" stroke=\"grey\" " +
                        "stroke-width=\"1\"  fill=\"transparent\"  stroke-dasharray=\"5,5\"/>\n",
                centerx, centery, radius*0.75));
        sb.append(String.format(Locale.ROOT, "<circle cx=\"%f\" cy=\"%f\" r=\"%f\" stroke=\"grey\" " +
                        "stroke-width=\"1\"  fill=\"transparent\"  stroke-dasharray=\"5,5\"/>\n",
                centerx, centery, radius*0.5));
        sb.append(String.format(Locale.ROOT, "<circle cx=\"%f\" cy=\"%f\" r=\"%f\" stroke=\"grey\" " +
                        "stroke-width=\"1\"  fill=\"transparent\"  stroke-dasharray=\"5,5\"/>\n",
                centerx, centery, radius*0.25));

        for(int angle=0; angle < 360; angle += 30) {
            double destX = centerx + Math.cos(toRadian(angle)) * radius;
            double destY = centery + Math.sin(toRadian(angle)) * radius;
            // Reverse order and rotate by 90°
            int adjustedAngle = getAdjustedAngle(angle, orientation);
            if(angle % 90 != 0) {
                // Dashed segment lines
                generateDashedLine(sb, centerx, centery, destX, destY, "grey");
            } else {
                // Plain segment lines
                generateLine(sb, centerx, centery, destX, destY, "grey");
            }
            double textX = centerx + Math.cos(toRadian(angle)) * textRadius;
            double textY = centery + Math.sin(toRadian(angle)) * textRadius;
            generateText(sb, textX, textY, 25, String.format(Locale.ROOT, "%d", adjustedAngle), "middle");
        }
        // Attenuation levels legend
        double legendAngle = 285;
        generateLegend(sb, minimumAttenuation, 0, legendAngle);
        generateLegend(sb, minimumAttenuation + (maximumAttenuation - minimumAttenuation) * 0.25,
                0.25, legendAngle);
        generateLegend(sb, minimumAttenuation + (maximumAttenuation - minimumAttenuation) * 0.5,
                0.5, legendAngle);
        generateLegend(sb, minimumAttenuation + (maximumAttenuation - minimumAttenuation) * 0.75,
    0.75, legendAngle);
        generateLegend(sb, maximumAttenuation, 1, legendAngle);
        // Generate attenuation curve
        StringBuilder path = new StringBuilder();
        for(int angle=0; angle < 360; angle += 1) {
            int adjustedAngle = getAdjustedAngle(angle, orientation);
            double phi=0;
            double theta=0;
            if(orientation == ORIENTATION.TOP) {
                phi = toRadian(adjustedAngle);
                theta = 0;
            } else if(orientation == ORIENTATION.FRONT) {
                if(angle <= 270) {
                    phi = toRadian(90);
                } else {
                    phi = toRadian(270);
                }
                theta = Math.sin(toRadian(adjustedAngle + 90)) * Math.PI / 2 ;
            } else if(orientation == ORIENTATION.SIDE) {
                if(angle <= 270) {
                    phi = toRadian(0);
                } else {
                    phi = toRadian(180);
                }
                theta = Math.sin(toRadian(adjustedAngle)) * Math.PI / 2 ;
            }
            double attenuation = RailWayLW.getDirectionAttenuation(noiseSource, phi, theta, frequency);
            double maxLevelX = centerx + Math.cos((angle / 180.0) * Math.PI) * radius;
            double maxLevelY = centery + Math.sin((angle / 180.0) * Math.PI) * radius;
            double attenuationPercentage = (attenuation - minimumAttenuation) / (maximumAttenuation - minimumAttenuation);
            Vector2D interpolatedVector = Vector2D.create(new Coordinate(centerx, centery), new Coordinate(maxLevelX, maxLevelY));
            interpolatedVector = interpolatedVector.multiply(attenuationPercentage);
            double stopX = centerx + interpolatedVector.getX();
            double stopY = centery + interpolatedVector.getY();
            if(angle == 0) {
                path.append(String.format(Locale.ROOT, "M %.1f %.1f", stopX, stopY));
            } else {
                path.append(String.format(Locale.ROOT, " L %.1f %.1f", stopX, stopY));
            }
            if(angle % 30 == 0) {
                path.append("\n");
            }
        }
        path.append(" Z");
        sb.append("<path d=\"");
        sb.append(path.toString());
        sb.append("\"  fill=\"transparent\" stroke=\"red\" stroke-width=\"2\"/>\n");
        sb.append("</svg> \n");
        return sb.toString();
    }

    public double getDwidth() {
        return dwidth;
    }

    public void setDwidth(double dwidth) {
        this.dwidth = dwidth;
        centerx = dwidth/2;
    }

    public double getDheight() {
        return dheight;
    }

    public void setDheight(double dheight) {
        this.dheight = dheight;
        centery = dheight/2;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getTextRadius() {
        return textRadius;
    }

    public void setTextRadius(double textRadius) {
        this.textRadius = textRadius;
    }

    public double getCenterx() {
        return centerx;
    }

    public void setCenterx(double centerx) {
        this.centerx = centerx;
    }

    public double getCentery() {
        return centery;
    }

    public void setCentery(double centery) {
        this.centery = centery;
    }
}
