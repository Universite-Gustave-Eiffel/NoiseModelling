package lcpc_son;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.geom.Coordinate;

public class MirrorReceiverResult {
	
	private Coordinate receiverPos;	//Coordinate of mirrored receiver
	private int mirrorResultId=-1;	//Other MirrorReceiverResult index, -1 for the first reflexion
	private int wallId=0;			//Wall index of the last mirrored processed
	
	public Coordinate getReceiverPos() {
		return receiverPos;
	}
	public void setReceiverPos(Coordinate receiverPos) {
		this.receiverPos = receiverPos;
	}
	public int getMirrorResultId() {
		return mirrorResultId;
	}
	public void setMirrorResultId(int mirrorResultId) {
		this.mirrorResultId = mirrorResultId;
	}
	public int getWallId() {
		return wallId;
	}
	public void setWallId(int wallId) {
		this.wallId = wallId;
	}
	public MirrorReceiverResult(Coordinate receiverPos, int mirrorResultId,
			int wallId) {
		super();
		this.receiverPos = receiverPos;
		this.mirrorResultId = mirrorResultId;
		this.wallId = wallId;
	}
		
		

}
