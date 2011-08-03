
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


package org.noisemap.run;

import java.io.File;
import org.gdms.driver.DriverException;
import org.gdms.driver.ReadAccess;
import org.gdms.driver.gdms.GdmsDriver;
import java.util.Stack;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.FunctionException;
import org.noisemap.core.BR_TriGrid;

/**
 * Independant run of BR_TriGrid plugin
 * Take two gdms file as input. The single output is another gdms file.
 */
public class trigrid {

    private static void printUsage() {
        System.out.println("BR_TriGrid version 03/08/2011 16:52");
        System.out.println("Usage :");
        System.out.println("java trigrid.jar [options] -ib bpath -is spath -o outpath");
        System.out.println("Options :");
        System.out.println("-bfield the_geom : buildings column name (polygons)");
        System.out.println("-sfield the_geom : sources column nale (points or lines)");
        System.out.println("-splfield db_m   : sound lvl field name(string)");
        System.out.println("-maxdist 170     : maximum propagation distance (double meter)");
        System.out.println("-maxrdist 50     : maximum wall reflexion distance (double meter)");
        System.out.println("-splitdepth 3    : subdivision level 4^n cells (int) [0-n]");
        System.out.println("-rwidth 0.8      : roads width (double meter)");
        System.out.println("-dense 5         : densification of receivers near roads (meter double)");
        System.out.println("-marea 250       : maximum area of triangle (square meter)");
        System.out.println("-rdepth 2        : sound reflection order [0-n] (int)");
        System.out.println("-ddepth 1        : sound diffraction order [0-n] (int)");
        System.out.println("-awalls 0.2      : alpha of walls [0-1[ (double)");
        System.out.println("-ib builds.gdms  : file name of buildings gdms file");
        System.out.println("-is sources.gdms : file name of noise sources gdms file");
        System.out.println("-o trilvl.gdms   : output filename of gdms file");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Parameters
        String buildingsFilename="";
        String sourcesFilename="";
        String outputFilename="";
        //Optionnal parameters
        String bField="the_geom";
        String sField="the_geom";
        String splField="db_m";
        double maxDist=170;
        double maxRDist=50;
        int splitDepth=3;
        double roadsWidth=0.8;
        double densification=5.;
        double maxarea=250;
        int reflectionDepth=2;
        int diffractionDepth=1;
        double wallAlpha=.2;


        //Read parameters
        Stack<String> sargs=new Stack<String>();
        for(String arg : args) {
            sargs.insertElementAt(arg, 0);
        }
        while(!sargs.empty()) {
            String argument=sargs.pop();
            System.out.println(argument+sargs);
            if(argument.contentEquals("-bfield")) {
                bField=sargs.pop();
            }else if(argument.contentEquals("-sfield")) {
                sField=sargs.pop();
            }else if(argument.contentEquals("-splfield")) {
                splField=sargs.pop();
            }else if(argument.contentEquals("-maxdist")) {
                maxDist=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-maxrdist")) {
                maxRDist=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-splitdepth")) {
                splitDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-rwidth")) {
                roadsWidth=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-dense")) {
                densification=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-marea")) {
                maxarea=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-rdepth")) {
                reflectionDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-ddepth")) {
                reflectionDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-awalls")) {
                wallAlpha=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-ib")) {
                buildingsFilename=sargs.pop();
            }else if(argument.contentEquals("-is")) {
                sourcesFilename=sargs.pop();
            }else if(argument.contentEquals("-o")) {
                outputFilename=sargs.pop();
            }else{
                System.err.println("Unknown parameter :"+argument);
                printUsage();
                return;
            }
        }
        if(buildingsFilename.isEmpty() || sourcesFilename.isEmpty() || outputFilename.isEmpty()) {
            printUsage();
            return;
        }
        //Load files
        SQLDataSourceFactory factory=new SQLDataSourceFactory();
        GdmsDriver buildings=new GdmsDriver();
        GdmsDriver sources=new GdmsDriver();
        ReadAccess[] tables={null,null};
        try {
            buildings.setFile(new File(buildingsFilename));
            buildings.open();
            tables[0]=buildings.getTable("main");
            sources.setFile(new File(buildingsFilename));
            sources.open();
            tables[1]=sources.getTable("main");
        } catch (DriverException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            return;
        }
        //Run propagation

        BR_TriGrid propa=new BR_TriGrid();
        propa.setLogger(new ConsoleLogger("BR_TriGrid"));
        Value[] propaArgs={ValueFactory.createValue(bField),ValueFactory.createValue(sField),ValueFactory.createValue(splField),ValueFactory.createValue(maxDist),ValueFactory.createValue(maxRDist),ValueFactory.createValue(splitDepth),ValueFactory.createValue(roadsWidth),ValueFactory.createValue(densification),ValueFactory.createValue(maxarea),ValueFactory.createValue(reflectionDepth),ValueFactory.createValue(diffractionDepth),ValueFactory.createValue(wallAlpha)};
        try {
            ReadAccess data = propa.evaluate(factory, tables, propaArgs, null);
        } catch (FunctionException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            return;
        }
    }

}
