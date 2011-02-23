package lcpc_son;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
/**
 * This class append an index to the envelope class
 * @param <index_t>
 */
public class EnvelopeWithIndex<index_t>  extends Envelope {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8552159007637756012L;
	private index_t index;

	public EnvelopeWithIndex(Coordinate p,index_t id) {
		super(p);
		index=id;
	}

	public EnvelopeWithIndex(Envelope env,index_t id) {
		super(env);
		index=id;
	}

	public EnvelopeWithIndex(Coordinate p1, Coordinate p2,index_t id) {
		super(p1, p2);
		index=id;
	}

	public EnvelopeWithIndex(double x1, double x2, double y1, double y2,index_t id) {
		super(x1, x2, y1, y2);
		index=id;
	}
	public index_t getId(){
		return index;
	}
	public Coordinate GetPosition()
	{
		return super.centre();
	}
	public void setId(index_t id){
		index=id;
	}

}
