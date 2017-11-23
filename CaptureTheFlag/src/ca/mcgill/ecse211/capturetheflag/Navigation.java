/**
 * Navigation.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * 
 * Navigation class which navigates robot to specified points, 
 * determines direction to turn (Minimal turn) and how far to travel
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */
public class Navigation {
		
	private static final int FORWARD_SPEED = 200;
	private static final int ROTATE_SPEED = 150;

	private static final int SLOW_ACCEL = 200;
	private static final int FAST_ACCEL = 300;
	
	private final GameParameters gameParameters;
	
	private double interruptedTheta = -1;
	
	//Motors
	private EV3LargeRegulatedMotor rightMotor;
	private EV3LargeRegulatedMotor leftMotor;
	
	//Associations
	private Odometer odometer;
	
	//Robot constants
	private final double WHEEL_RADIUS;
	private final double TRACK;
	
	/**
	 * Creates a Navigation object.
	 * @param odometer  Association to Odometer instance
	 * @param rightMotor  Reference to the right motor
	 * @param leftMotor  Reference to the left motor
	 * @param wheelRadius  Radius of the wheels of the robot (cm)
	 * @param track  Radius of the wheelbase of the robot (cm)
	 * @param gameParameters  The game parameters for this round
	 */
	public Navigation(Odometer odometer, EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, double wheelRadius, double track, GameParameters gameParameters) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.odometer = odometer;
		
		this.WHEEL_RADIUS = wheelRadius;
		this.TRACK = track;
		this.gameParameters = gameParameters;
	}

	/**
	 * This method causes the robot to travel to the absolute field location (x,
	 * y), specified in tilepoints by first adjusting its heading then moving forward.
	 * The returnThread boolean is used to determine if the thread should wait until the robot is done traveling to return
	 * @param x  The target X position to travel to
	 * @param y  The target Y position to travel to
	 * @param returnThread  If true the thread does not wait for the travel to complete
	 */
	public void travelTo(double x, double y, boolean returnThread) {		
		
		double currentX = odometer.getX();
		double currentY = odometer.getY();
		
		double nextHeading = Math.toDegrees(Math.atan2(x - currentX, y - currentY));

		turnTo(nextHeading);
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			
		}
	    
	    double distance = Math.sqrt(Math.pow((y - currentY),2) + Math.pow((x - currentX),2));
	    
	    forward(distance, returnThread);
	    
	   
	}
	
	/**
	 * Makes the robot turn (minimal angle) to a new heading.
	 * @param theta  Target heading to turn to
	 */
	public void turnTo(double theta) {
		leftMotor.setSpeed(ROTATE_SPEED);
	    rightMotor.setSpeed(ROTATE_SPEED);
		double currentTheta = odometer.getThetaDegrees();
		double rightRotation = theta - currentTheta;
		if (rightRotation < 0) {
			rightRotation = rightRotation + 360;
		}
		double leftRotation = currentTheta - theta;
		if (leftRotation < 0) {
			leftRotation = leftRotation + 360;
		}
		rightMotor.setAcceleration(SLOW_ACCEL);
		leftMotor.setAcceleration(SLOW_ACCEL);
		if (rightRotation < leftRotation) {
			leftMotor.rotate(convertAngle(WHEEL_RADIUS, TRACK, rightRotation), true);
		    rightMotor.rotate(-convertAngle(WHEEL_RADIUS, TRACK, rightRotation), false);
		}
		else {
			leftMotor.rotate(-convertAngle(WHEEL_RADIUS, TRACK, leftRotation), true);
		    rightMotor.rotate(convertAngle(WHEEL_RADIUS, TRACK, leftRotation), false);
		}
		rightMotor.setAcceleration(FAST_ACCEL);
		leftMotor.setAcceleration(FAST_ACCEL);
	}
	
	/**
	 * Makes the robot face the zipline
	 */
	public void faceZipline() {
		turnTo(Math.toDegrees(Math.atan2(gameParameters.ZC_G.x - gameParameters.ZO_G.x ,  gameParameters.ZC_G.y - gameParameters.ZO_G.y)));
	}
	
	/**
	 * Makes the robot move forward by a certain distance.
	 * The returnThread boolean is used to determine if the thread should wait until the robot is done traveling to return.
	 * @param distance  Distance to travel forward
	 * @param returnThread  If true the thread does not wait for the travel to complete
	 */
	public void forward(double distance, boolean returnThread) {
		leftMotor.setSpeed(FORWARD_SPEED);
	    rightMotor.setSpeed(FORWARD_SPEED);
	    rightMotor.setAcceleration(FAST_ACCEL);
		leftMotor.setAcceleration(FAST_ACCEL);
		leftMotor.rotate(convertDistance(WHEEL_RADIUS, distance), true);
	    rightMotor.rotate(convertDistance(WHEEL_RADIUS, distance), returnThread);
	}
	
	/**
	 * This method interrupts the current navigation of the robot. This interruption
	 * is likely due it the ultrasonic sensor detecting an object in the robots trajectory.
	 * @param turnRight  If true the robot turns right 90° upon interruption, if false it turn left by 90°
	 */
	public void interruptNav(boolean turnRight) {
		rightMotor.stop(true);
		leftMotor.stop();
		int polarity = 1;
		if (!turnRight) {
			polarity = -1;
		}
		leftMotor.rotate(polarity * convertAngle(WHEEL_RADIUS, TRACK, 90), true); //when an object is detected, we turn to starting to follow it as a wall.
	    rightMotor.rotate(- polarity * convertAngle(WHEEL_RADIUS, TRACK, 90), false);
	    
	    setInterruptedTheta((int) odometer.getThetaDegrees());
	}

	/**
	 * Converts a distance to wheel rotation.
	 * @param radius  Radius of the wheel
	 * @param distance  Distance to convert
	 * @return  Wheel rotation in degrees
	 */
	public int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	/**
	 * Converts distance to wheel rotation required for the physical robot to turn by a certain
	 * angle around its center of rotation.
	 * @param radius  Radius of the wheel
	 * @param width  Width of the wheelbase
	 * @param angle  Angle turn wanted
	 * @return  Wheel rotation in degrees
	 */
	public int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
	
	/**
	 * Saves the heading at which the navigation got interrupted.
	 * @param theta  New theta to be set
	 */
	public void setInterruptedTheta(double theta) {
		this.interruptedTheta = theta;
	}
	
	/**
	 * Returns the heading at which the navigation got interrupted.
	 * @return  Current theta at which navigation was interrupted
	 */
	public double getInterruptedTheta() {
		return interruptedTheta;
	}

	
	
}
