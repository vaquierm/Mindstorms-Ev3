package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Navigation {
	
	private Object lock = new Object();
	private static final int FORWARD_SPEED = 250;
	private static final int ROTATE_SPEED = 150;
	
	private boolean navigating = false;
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	private double leftRadius;
	private double rightRadius;
	private double width;


	private final int[][] points = { { 2, 1 }, { 1, 2 }, { 1, 1 }, { 2, 0 } };

	public Navigation(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double leftRadius,
			double rightRadius, double width) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.leftRadius = leftRadius;
		this.rightRadius = rightRadius;
		this.width = width;
	}

	/*
	 * This method causes the robot to travel to the absolute field location (x,
	 * y), specified in tilepoints.This method should continuously
	 * callturnTo(double theta)and thenset the motor speed to forward(straight).
	 * This will make sure that yourheading is updated until you reach your
	 * exact goal. This method will pollthe odometer for informatio
	 */
	private void travelTo(int x, int y) {
		
	}
	
	public void turnTo(double theta) {
		setNavigating(true);
		leftMotor.setSpeed(ROTATE_SPEED);
	    rightMotor.setSpeed(ROTATE_SPEED);
		double currentTheta = odometer.getThetaDegrees();
		double rightRotation = theta - currentTheta;
		if (rightRotation < 0) {
			rightRotation = rightRotation + 360;
		}
		double leftRotation = theta - currentTheta;
		if (leftRotation > 0) {
			leftRotation = leftRotation - 360;
		}
		leftRotation = -leftRotation;
		
		if (rightRotation < leftRotation) {
			leftMotor.rotate(convertAngle(leftRadius, width, rightRotation), true);
		    rightMotor.rotate(-convertAngle(rightRadius, width, rightRotation), false);
		}
		else {
			leftMotor.rotate(-convertAngle(leftRadius, width, leftRotation), true);
		    rightMotor.rotate(convertAngle(rightRadius, width, leftRotation), false);
		}
		setNavigating(false);
	}


	private int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	private int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}
	
	public boolean isNavigating() {
		synchronized(lock) {
			return navigating;
		}
	}
	
	private void setNavigating(boolean nav) {
		synchronized (lock) {
			navigating = nav;
		}
	}
}
