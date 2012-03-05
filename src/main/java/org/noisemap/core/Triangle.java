package org.noisemap.core;

/***********************************
 * ANR EvalPDU IFSTTAR 11_05_2011
 * 
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

/*
 * A triangle built from the combination of the 3 vertices index.
 */
public class Triangle {
	private int a = 0;
	private int b = 0;
	private int c = 0;

	public int getA() {
		return a;
	}

	public int get(int id) {
		switch (id) {
		case 0:
			return a;
		case 1:
			return b;
		default:
			return c;
		}
	}

	public void set(int id,int index) {
		switch (id) {
		case 0:
			a=index;
			break;
		case 1:
			b=index;
			break;
		default:
			c=index;
		}
	}

	public void setA(int a) {
		this.a = a;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}

	public int getC() {
		return c;
	}

	public void setC(int c) {
		this.c = c;
	}

	/**
	 * Get triangle side (a side is the opposite of vertex index
         *    0
         *   /\ 
         * c/  \ b
	 * /____\ 
         *1  a   2
	 * 
	 * @param side
	 * @return
	 */
	public IntSegment getSegment(int side) {
		switch (side) {
		default:
		case 0: // a side
			return new IntSegment(this.b, this.c);
		case 1: // b side
			return new IntSegment(this.c, this.a);
		case 2: // c side
			return new IntSegment(this.a, this.b);
		}
	}

	public Triangle(int a, int b, int c) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
