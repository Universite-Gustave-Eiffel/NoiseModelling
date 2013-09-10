package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import static junit.framework.Assert.assertFalse;

import static junit.framework.Assert.assertTrue;


import junit.framework.TestCase;

/**
 *
 * @author SU Qi
 */
public class TestSoilEffet extends TestCase{
    
    public void testSoilEffet() throws LayerDelaunayError{
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                        new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                        new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
        
        
        
        Coordinate topoPoint1 = new Coordinate(5.,10.,1.);
        Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
        Coordinate[] geoSoil1={new Coordinate(7.,5.,0.), new Coordinate(7.,30.,0.),
                               new Coordinate(10.,13.,0.), new Coordinate(14.,30.,0.), 
                               new Coordinate(14.,5.,0.), new Coordinate(7.,5.,0.)}; 
        MeshBuilder mesh= new MeshBuilder();
        mesh.addGeometry(building1,6.0);
        mesh.addTopograhicPoint(topoPoint1);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));

        FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
        
        nfot.addGeoSoil(factory.createPolygon(geoSoil1), 0.7);
        nfot.isCalSoilEffet=true;
        nfot.checkGeoSoil=true;
        System.out.println("----------TEST#1 diffraction with 1 buildings----- ");

        Double[]lt=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));
        System.out.println("----deltadistance----");
        System.out.println(lt[nfot.Delta_Distance]);
        System.out.println("----e----");
        System.out.println(lt[nfot.E_Length]);
        System.out.println("----distancepath----");
        System.out.println(lt[nfot.Full_Difrraction_Distance]);
        System.out.println("----full distance with soil effet----");
        System.out.println(lt[nfot.Full_Distance_With_Soil_Effet]);    
    }
}
