package org.noise_planet.noisemodelling.pathfinder.utils;

import org.locationtech.jts.geom.Coordinate;

public class MirrorReflection2D {

   /* public static void main(String[] args) {
        // Points 3D
        Coordinate point = new Coordinate(10, 10, 1); // Point à réfléchir
        Coordinate referencePoint = new Coordinate(170, 60, 15); // Point de référence

        // Calcul des coordonnées miroir en 2D
        Coordinate mirroredPoint = getMirroredPoint2D(point, referencePoint);

        // Affichage des résultats
        System.out.println("Point d'origine: " + point);
        System.out.println("Point de référence: " + referencePoint);
        System.out.println("Point miroir en 2D: " + mirroredPoint);
    }

    /**
     * Calcule les coordonnées miroir en 2D d'un point 3D donné par rapport à un point 3D donné.
     * @param point Le point à réfléchir.
     * @param referencePoint Le point de référence pour la réflexion.
     * @return Les coordonnées miroir en 2D du point donné.

    public static Coordinate getMirroredPoint2D(Coordinate point, Coordinate referencePoint) {
        double mirroredX = 2 * referencePoint.x - point.x;
        double mirroredY = 2 * referencePoint.y - point.y;
        double mirroredZ = point.z; // La composante z reste inchangée pour la réflexion 2D

        return new Coordinate(mirroredX, mirroredY, mirroredZ);
    }

    public static void main(String[] args) {
        Coordinate point3D = new Coordinate(10, 10, 1);
        Coordinate mirroredPoint = getMirroredCoordinate2D(point3D, "X");
        System.out.println("Original Point: " + point3D);
        System.out.println("Mirrored Point: " + mirroredPoint);
    }

    /**
     * Classe représentant une coordonnée 3D.

    static class Coordinate {
        double x, y, z;

        Coordinate(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "Coordinate{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
        }
    }

    /**
     * Calcule les coordonnées miroir en 2D d'un point 3D donné.
     *
     * @param point3D Le point 3D d'origine.
     * @param axis    L'axe par rapport auquel le point est réfléchi ("X" ou "Y").
     * @return Les coordonnées miroir en 2D.

    public static Coordinate getMirroredCoordinate2D(Coordinate point3D, String axis) {
        // Projeter le point 3D sur le plan 2D (ici on ignore la coordonnée z)
        double x = point3D.x;
        double y = point3D.y;

        // Calculer les coordonnées miroir par rapport à l'axe spécifié
        switch (axis) {
            case "X":
                y = -y;
                break;
            case "Y":
                x = -x;
                break;
            default:
                throw new IllegalArgumentException("L'axe doit être 'X' ou 'Y'");
        }

        return new Coordinate(x, y, 0);
    }*/


    public static void main(String[] args) {
        // Points à refléter
        Coordinate3D sourcePoint = new Coordinate3D(10, 10, 1);
        Coordinate3D receiverPoint = new Coordinate3D(200, 50, 14);

        // Points de référence de l'écran
        Coordinate3D screenPoint1 = new Coordinate3D(114, 52, 15);
        Coordinate3D screenPoint2 = new Coordinate3D(170, 60, 15);

        // Calcul des coordonnées miroir en utilisant la méthode correcte
        Coordinate2D mirroredSource = getMirrorCoordinate2D(sourcePoint, screenPoint1, screenPoint2);
        Coordinate2D mirroredReceiver = getMirrorCoordinate2D(receiverPoint, screenPoint1, screenPoint2);

        // Affichage des résultats
        System.out.println("Mirrored Source: (" + mirroredSource.u + ", " + mirroredSource.v + ")");
        System.out.println("Mirrored Receiver: (" + mirroredReceiver.u + ", " + mirroredReceiver.v + ")");
    }

    public static Coordinate2D getMirrorCoordinate2D(Coordinate3D point, Coordinate3D screenPoint1, Coordinate3D screenPoint2) {
        // Projeter les points dans le plan xz
        Coordinate2D p = new Coordinate2D(point.x, point.z);
        Coordinate2D sp1 = new Coordinate2D(screenPoint1.x, screenPoint1.z);
        Coordinate2D sp2 = new Coordinate2D(screenPoint2.x, screenPoint2.z);

        // Calculer les coordonnées miroir
        return reflectPointOverLine(p, sp1, sp2);
    }

    public static Coordinate2D reflectPointOverLine(Coordinate2D p, Coordinate2D a, Coordinate2D b) {
        // Calculer les différences
        double dx = b.u - a.u;
        double dz = b.v - a.v;

        // Calculer les paramètres pour la ligne
        double A = dz;
        double B = -dx;
        double C = dx * a.v - dz * a.u;

        // Calculer la distance perpendiculaire du point à la ligne
        double dist = (A * p.u + B * p.v + C) / Math.sqrt(A * A + B * B);

        // Calculer les coordonnées miroir
        double mirroredU = p.u - 2 * A * dist / (A * A + B * B);
        double mirroredV = p.v - 2 * B * dist / (A * A + B * B);

        return new Coordinate2D(mirroredU, mirroredV);
    }
}

    // Classe pour les coordonnées 3D
    class Coordinate3D {
        double x, y, z;

        public Coordinate3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // Classe pour les coordonnées 2D (u, v)
    class Coordinate2D {
        double u, v;

        public Coordinate2D(double u, double v) {
            this.u = u;
            this.v = v;
        }
    }
