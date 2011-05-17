package lcpc_son;
/***********************************
 * ANR EvalPDU
 * Lcpc 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


/**
 * Throwed delaunay error
 */


public class LayerDelaunayError extends Throwable {
	private static final long serialVersionUID = 1L;

	// error code saving
	String errorMessage;

	public LayerDelaunayError(String ErrorMsg) {
		super();
		errorMessage=ErrorMsg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return errorMessage;
	}
}
