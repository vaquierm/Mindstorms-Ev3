/**
 * Coordinate.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * Simple class to represent the coordinates where 
 * the robot will travel
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */
public class Coordinate {
	public final double x;
	public final double y;
	
	/**
	 * Creates a Coordinate Object
	 * @param x
	 * @param y
	 */
	public Coordinate(double x, double y) {
		this.x = x;
		this.y = y;
	}

}
