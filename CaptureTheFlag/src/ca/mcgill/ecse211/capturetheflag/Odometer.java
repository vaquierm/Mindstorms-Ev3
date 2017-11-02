/**
 * Odometer.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * 
 * Odometer can spawn a thread to keep track of wheel rotation to have
 * a good estimate of the position of the center of rotation of the robot.
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */
public class Odometer implements Runnable {
	// robot position
	private double x;
	private double y;
	private double theta;
	private double thetaDegree;
	private int leftMotorTachoCount;
	private int rightMotorTachoCount;
	
	//Motors
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	
	//Robot constants
	private final double WHEEL_RADIUS;
	private final double TRACK;


	private static final long ODOMETER_PERIOD = 15; 

	private Object lock; /* lock object for mutual exclusion */

	/**
	 * Constructs an Odometer object.
	 * @param leftMotor  Reference to the left motor
	 * @param rightMotor  Reference to the right motor
	 * @param wheelRadius  Radius of the wheels
	 * @param track  Width of the wheelbase
	 */
	public Odometer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, double wheelRadius, double track) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.WHEEL_RADIUS = wheelRadius;
		this.TRACK = track;
		this.x = 0.0;
		this.y = 0.0;
		this.theta = 0.0;
		this.thetaDegree = 0.0;
		this.leftMotorTachoCount = 0;
		this.rightMotorTachoCount = 0;
		lock = new Object();
	}

	/**
	 * Runs the odometer, to periodically poll the tachocount of the wheels
	 * to update the current position of the robot.
	 */
	public void run() {
		long updateStart, updateEnd;

		while (true) {
			updateStart = System.currentTimeMillis();
			double distL, distR, deltaD, deltaT, dX, dY;
			int nowTachoL = leftMotor.getTachoCount();
			int nowTachoR = rightMotor.getTachoCount();
			distL = Math.PI * WHEEL_RADIUS * (nowTachoL - leftMotorTachoCount) / 180;
			distR = Math.PI * WHEEL_RADIUS * (nowTachoR - rightMotorTachoCount) / 180;
			leftMotorTachoCount = nowTachoL;
			rightMotorTachoCount = nowTachoR;
			deltaD = 0.5 * (distL + distR);
			deltaT = (distL - distR) / TRACK;

			synchronized (lock) {
				setTheta(theta + deltaT);
				dX = deltaD * Math.sin(theta);
				dY = deltaD * Math.cos(theta);
				x = x + dX;
				y = y + dY;
			}

			// this ensures that the odometer only runs once every period
			updateEnd = System.currentTimeMillis();
			if (updateEnd - updateStart < ODOMETER_PERIOD) {
				try {
					Thread.sleep(ODOMETER_PERIOD - (updateEnd - updateStart));
				} catch (InterruptedException e) {
					
				}
			}
		}
	}

	/**
	 * Writes the current position of the robot in the input array only at the positions
	 * where an update is requested.
	 * The indices of the array corresponds to the positions as such:
	 * 0: x
	 * 1: y
	 * 2: T
	 * @param position  Array to be written to
	 * @param update  Array indicating which indices to write to
	 */
	public void getPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (position.length == 3 && update.length == 3) {
				if (update[0])
					position[0] = x;
				if (update[1])
					position[1] = y;
				if (update[2])
					position[2] = thetaDegree;
			}
		}
	}

	/**
	 * Gets the current X value stored in the odometer.
	 * @return  The current X value of the odometer
	 */
	public double getX() {
		double result;

		synchronized (lock) {
			result = x;
		}

		return result;
	}

	/**
	 * Gets the current Y value stored in the odometer.
	 * @return  The current Y value of the odomeer
	 */
	public double getY() {
		double result;

		synchronized (lock) {
			result = y;
		}

		return result;
	}

	/**
	 * Gets the current heading value stored in the odometer in radiant.
	 * @return  The current heading of the odometer in radiant
	 */
	public double getTheta() {
		double result;

		synchronized (lock) {
			result = theta;
		}

		return result;
	}
	
	/**
	 * Gets the current heading value stored in the odometer in degrees.
	 * @return  The current heading of the odometer in degrees
	 */
	public double getThetaDegrees() {
		double result;

		synchronized (lock) {
			result = thetaDegree;
		}

		return result;
	}

	
	/**
	 * Writes the current positions from the input array
	 * to the robot's odometer where an update is requested.
	 * The indices of the array corresponds to the positions as such:
	 * 0: x
	 * 1: y
	 * 2: T
	 * @param position  The array to read from
	 * @param update  Array indicating which indices to read from
	 */
	public void setPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if(update.length == 3 && position.length == 3) {
				if (update[0])
					x = position[0];
				if (update[1])
					y = position[1];
				if (update[2])
					theta = position[2];
			}
		}
	}

	/**
	 * Sets the current X value of the odometer.
	 * @param x  New X value
	 */
	public void setX(double x) {
		synchronized (lock) {
			this.x = x;
		}
	}

	/**
	 * Sets the current Y value of the odometer.
	 * @param y  New Y value
	 */
	public void setY(double y) {
		synchronized (lock) {
			this.y = y;
		}
	}
	
	/**
	 * Converts an angle in radiant to degrees.
	 * @param rad  Angle in radiant to convert
	 * @return  Angle in degrees
	 */
	public double radiantToDegree(double rad) {
		return rad * 180 / Math.PI;
	}

	/**
	 * Sets the current heading of the robot in the odometer.
	 * @param theta  New theta in radiant to be set in the odometer
	 */
	public void setTheta(double theta) {
		while (theta < 0) {
			theta += 2 * Math.PI;
		}
		theta %= (2 * Math.PI);
		synchronized (lock) {
			this.thetaDegree = theta * 180 / Math.PI;
			this.theta = theta;
		}
	}

	/**
	 * @return the leftMotorTachoCount
	 */
	public int getLeftMotorTachoCount() {
		return leftMotorTachoCount;
	}

	/**
	 * @param leftMotorTachoCount
	 *            the leftMotorTachoCount to set
	 */
	public void setLeftMotorTachoCount(int leftMotorTachoCount) {
		synchronized (lock) {
			this.leftMotorTachoCount = leftMotorTachoCount;
		}
	}

	/**
	 * @return the rightMotorTachoCount
	 */
	public int getRightMotorTachoCount() {
		return rightMotorTachoCount;
	}

	/**
	 * @param rightMotorTachoCount
	 *            the rightMotorTachoCount to set
	 */
	public void setRightMotorTachoCount(int rightMotorTachoCount) {
		synchronized (lock) {
			this.rightMotorTachoCount = rightMotorTachoCount;
		}
	}
}
