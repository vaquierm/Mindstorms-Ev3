package ca.mcgill.ecse211.capturetheflag;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * 
 * Navigation class which navigates robot to specified points, 
 * determines direction to turn (Minimal turn) and how far to travel
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class Navigation {
		
	private static final int FORWARD_SPEED = 150;
	private static final int ROTATE_SPEED = 140;

	private static final int SLOW_ACCEL = 250;
	private static final int FAST_ACCEL = 500;
	
	private double interruptedTheta = -1;
	
	//Motors
	private EV3LargeRegulatedMotor rightMotor;
	private EV3LargeRegulatedMotor leftMotor;
	
	//Associations
	private Odometer odometer;
	
	//Robot constants
	private final double WHEEL_RADIUS;
	private final double TRACK;
	

	public Navigation(Odometer odometer, EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, double wheelRadius, double track) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.odometer = odometer;
		
		this.WHEEL_RADIUS = wheelRadius;
		this.TRACK = track;
	}

	/*
	 * This method causes the robot to travel to the absolute field location (x,
	 * y), specified in tilepoints.This method should continuously
	 * callturnTo(double theta)and thenset the motor speed to forward(straight).
	 * This will make sure that yourheading is updated until you reach your
	 * exact goal. This method will pollthe odometer for informatio
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
	
	public void forward(double distance, boolean returnThread) {
		leftMotor.setSpeed(FORWARD_SPEED);
	    rightMotor.setSpeed(FORWARD_SPEED);
		leftMotor.rotate(convertDistance(WHEEL_RADIUS, distance), true);
	    rightMotor.rotate(convertDistance(WHEEL_RADIUS, distance), returnThread);
	}
	
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

	public int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	public int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
	
	public void setInterruptedTheta(double theta) {
		this.interruptedTheta = theta;
	}
	
	public double getInterruptedTheta() {
		return interruptedTheta;
	}

	
	
}
