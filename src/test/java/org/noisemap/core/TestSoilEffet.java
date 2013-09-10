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
    
    public void testSoilEffet1() throws LayerDelaunayError{
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                        new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                        new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
        
        
        
        Coordinate topoPoint1 = new Coordinate(5.,10.,1.);
        Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
        Coordinate[] geoSoil1 = {new Coordinate(7.,5.,0.), new Coordinate(7.,30.,0.),
                               new Coordinate(10.,13.,0.), new Coordinate(14.,30.,0.), 
                               new Coordinate(14.,5.,0.), new Coordinate(7.,5.,0.)}; 
        MeshBuilder mesh= new MeshBuilder();
        mesh.addGeometry(building1,6.0);
        mesh.addTopograhicPoint(topoPoint1);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));

        FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
        
        nfot.addGeoSoil(factory.createPolygon(geoSoil1), 0.7);

        
        System.out.println("----------TEST#1 diffraction with 1 building last path has soil effet----- ");

        Double[]lt=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));


        
        System.out.println("----------TEST#1 diffraction with 1 building last path has soil effet finished----- ");
    }

    public void testSoilEffet2() throws LayerDelaunayError{
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] building1Coords = { new Coordinate(15., 5.,2.),
                                        new Coordinate(15., 30.,3.), new Coordinate(30., 30.,5.),
                                        new Coordinate(30., 5.,2.), new Coordinate(15., 5.,2.) };
        
        
        
        Coordinate topoPoint1 = new Coordinate(5.,10.,1.);
        Polygon building1 = factory.createPolygon(
                            factory.createLinearRing(building1Coords), null);
        Coordinate[] geoSoil1 = {new Coordinate(7.,5.,0.), new Coordinate(7.,30.,0.),
                               new Coordinate(10.,13.,0.), new Coordinate(14.,30.,0.), 
                               new Coordinate(14.,5.,0.), new Coordinate(7.,5.,0.)};
        Coordinate[] geoSoil2 = {new Coordinate(35.,10.,0.), new Coordinate(35.,40.,0.),
                               new Coordinate(40.,40.,0.), new Coordinate(40.,10.,0.), 
                               new Coordinate(35.,10.,0.)};
        Coordinate[] geoSoil3 = {new Coordinate(45.,10.,0.), new Coordinate(45.,40.,0.),
                               new Coordinate(55.,40.,0.), new Coordinate(55.,10.,0.), 
                               new Coordinate(45.,10.,0.)};
        
        MeshBuilder mesh= new MeshBuilder();
        mesh.addGeometry(building1,6.0);
        mesh.addTopograhicPoint(topoPoint1);
        mesh.finishPolygonFeeding(new Envelope(new Coordinate(0., 0.,0.), new Coordinate(60., 60.,0.)));

        FastObstructionTest nfot= new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
        
        nfot.addGeoSoil(factory.createPolygon(geoSoil1), 0.7);
        //calculate soil effet

        //debug mode

        
        System.out.println("----------TEST#2 diffraction with 1 building first path and last path have soil effet----- ");

        Double[]lt=nfot.getPath(new Coordinate(48,25,7), new Coordinate(5,15,8));


        
        
    }    
}
