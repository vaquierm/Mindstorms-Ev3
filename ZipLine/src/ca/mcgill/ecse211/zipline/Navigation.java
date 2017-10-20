package ca.mcgill.ecse211.zipline;

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
	
	private Object lock = new Object();
	
	
	private static final int FORWARD_SPEED = 150;
	private static final int ROTATE_SPEED = 140;

	
	private static final int SLOW_ACCEL = 250;
	private static final int FAST_ACCEL = 500;
	
	private Odometer odometer;
	public EV3LargeRegulatedMotor leftMotor;
	public EV3LargeRegulatedMotor rightMotor;
	private double leftRadius;
	private double rightRadius;
	private double width;

	public Navigation(Odometer odometer, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double leftRadius,
		double rightRadius, double width) {
		this.odometer = odometer;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.leftRadius = leftRadius;
		this.rightRadius = rightRadius;
		this.width = width;
		this.leftMotor.setAcceleration(FAST_ACCEL);
		this.rightMotor.setAcceleration(FAST_ACCEL);
	}

	/*
	 * This method causes the robot to travel to the absolute field location (x,
	 * y), specified in tilepoints.This method should continuously
	 * callturnTo(double theta)and thenset the motor speed to forward(straight).
	 * This will make sure that yourheading is updated until you reach your
	 * exact goal. This method will pollthe odometer for informatio
	 */
	public void travelTo(double x, double y, boolean returnThread) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			
		}
		
		
		double currentX = odometer.getX();
		double currentY = odometer.getY();
		
		double nextHeading = Math.toDegrees(Math.atan2(x - currentX, y - currentY));

		turnTo(nextHeading);
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			
		}
		
		
		leftMotor.setSpeed(FORWARD_SPEED);
	    rightMotor.setSpeed(FORWARD_SPEED);
	    
	    double distance = Math.sqrt(Math.pow((y - currentY),2) + Math.pow((x - currentX),2));
	    
	    leftMotor.rotate(convertDistance(leftRadius, distance), true);
	    rightMotor.rotate(convertDistance(rightRadius, distance), returnThread);
	    
	   
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
			leftMotor.rotate(convertAngle(leftRadius, width, rightRotation), true);
		    rightMotor.rotate(-convertAngle(rightRadius, width, rightRotation), false);
		}
		else {
			leftMotor.rotate(-convertAngle(leftRadius, width, leftRotation), true);
		    rightMotor.rotate(convertAngle(rightRadius, width, leftRotation), false);
		}
		rightMotor.setAcceleration(FAST_ACCEL);
		leftMotor.setAcceleration(FAST_ACCEL);
	}
	
	public void turnTo(double x, double y) {
		double currentX = odometer.getX();
		double currentY = odometer.getY();
		
		double nextHeading = Math.toDegrees(Math.atan2(x - currentX, y - currentY));

		turnTo(nextHeading);
	}
	
	public void forward(double distance, boolean returnThread) {
		leftMotor.setSpeed(ROTATE_SPEED);
	    rightMotor.setSpeed(ROTATE_SPEED);
		leftMotor.rotate(convertDistance(leftRadius, distance), true);
	    rightMotor.rotate(convertDistance(rightRadius, distance), returnThread);
	}


	public int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	public int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

	
	
}
