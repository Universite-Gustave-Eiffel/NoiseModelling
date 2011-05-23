/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package lcpc_son;

/**
 * JAVA/C interface data structure
 */


public class CTriangleStruct {

	public void initOut()
	{
		initArrays(false,true);
	}
	public void initArrays(boolean in,boolean out) {
		//POINTS
		pointlist = new double[2*numberofpoints];
		pointattributelist = new double[numberofpoints*numberofpointattributes];
		pointmarkerlist = new int[numberofpoints];
		for(int idpt=0;idpt<numberofpoints;idpt++)
			pointmarkerlist[idpt]=0;
		
		//TRIANGLES
		trianglelist = new int[numberofcorners*numberoftriangles];
		triangleattributelist = new double[numberoftriangles*numberoftriangleattributes];
		if(in)
			trianglearealist = new double[numberoftriangles];
		if(out)
			neighborlist = new int[numberoftriangles*3*numberofcorners];
		
		//SEGMENTS
		segmentlist = new int[2*numberofsegments];
		segmentmarkerlist = new int[numberofsegments];
		for(int idmark=0;idmark<numberofsegments;idmark++)
			segmentmarkerlist[idmark]=0;
		//HOLES
		if(in)
			holelist  = new double[2*numberofholes];
		
		//REGIONS
		if(in)
			regionlist = new double[4*numberofregions];
	
		//EDGES
		if(out)
		{
			edgelist = new int[2*numberofedges];
			edgemarkerlist= new int[numberofedges];
			normlist = new double[2*numberofedges];
		}
	}
	public double[] pointlist = null;				//IN OUT
	public double[] pointattributelist = null;		//IN OUT
	public int[] pointmarkerlist = null;			//IN OUT
	public int numberofpoints = 0;					//IN OUT
	public int numberofpointattributes = 0;			//IN OUT

	public int[] trianglelist = null;				//IN OUT
	public double[] triangleattributelist = null; 	//IN OUT
	public double[] trianglearealist = null; 		//IN
	public int[] neighborlist = null;  				//OUT
	public int numberoftriangles = 0; 				//IN OUT
	public int numberofcorners = 3; 				//IN OUT
	public int numberoftriangleattributes = 0; 		//IN OUT

	public int[] segmentlist = null; 				//IN OUT
	public int[] segmentmarkerlist = null; 			//IN OUT
	public int numberofsegments = 0; 				//IN OUT

	public double[] holelist = null; 				//IN
	public int numberofholes = 0; 					//IN

	public double[] regionlist = null; 				//IN
	public int numberofregions = 0;					//IN

	public int[] edgelist = null;					//OUT
	public int[] edgemarkerlist = null;				//OUT
	public double[] normlist = null;				//OUT
	public int numberofedges = 0;					//OUT

}
