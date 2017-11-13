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
	
	/**
	 * X value of the Coordinate
	 */
	public final double x;
	
	/**
	 * Y value of the Coordinate
	 */
	public final double y;
	
	/**
	 * Creates a Coordinate Object
	 * @param x  The X value of the new coordinate
	 * @param y  The Y value of the new coordinate
	 */
	public Coordinate(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Compares two coordinates
	 * @param other  The other coordinate
	 * @return  Returns true if the two coordinates are the same.
	 */
	public boolean equals(Coordinate other) {
		if (other == null) {
			return false;
		}
		return (this.x == other.x) && (this.y == other.y);
	}

}
