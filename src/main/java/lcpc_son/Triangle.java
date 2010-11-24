package lcpc_son;

/*
 * A triangle built from the combination of the 3 vertices index.
 */
public class Triangle {
	private int a=0;
	private int b=0;
	private int c=0;
	public int getA() {
		return a;
	}
	public int get(int id)
	{
		switch (id) {
			case 0:
				return a;
			case 1:
				return b;
			default:
				return c;
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
	public Triangle(int a, int b, int c) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
