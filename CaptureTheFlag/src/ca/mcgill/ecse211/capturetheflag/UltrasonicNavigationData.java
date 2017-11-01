/**
 * UltrasonicNavidationData.java
 */

package ca.mcgill.ecse211.capturetheflag;

import ca.mcgill.ecse211.capturetheflag.NavigationController.NavigationState;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The UltrasonicNavigationData class processes the data coming from an ultrasonic sensor while the robot is navigating
 * in oder to avoid obstacles
 * 
 * @author Michael Vaquier
 *
 */

public class UltrasonicNavigationData {
	
	private static final int OBSTACLE_THRESHOLD = 18;
	
	
	//Motors
	EV3LargeRegulatedMotor rightMotor;
	EV3LargeRegulatedMotor leftMotor;
	
	//Associations
	private Navigation navigation;
	private NavigationController navigationController;
	private Odometer odometer;
	
	private boolean followingLeftWall = false;
	
	private static final int MOTOR_SPEED = 140;
	private static final int FILTER_OUT = 16;
	private static final int ERROR_PROPORTIONALITY = 35;
	private static final int CORNER_OUTER_SPEED = 250;
	private static final int CORNER_INNER_SPEED = 100;
	private static final int BACKWARD_SPEED = 200;
	private static final int SPIN_SPEED = 150;
	
	private final double tile;
	private final double boardSize;

	private final int bandCenter = 20;
	private final int bandWidth = 3;
	private int wheelSpeedDifference;
	private int distance;
	private int filterControl;
	private boolean corner = false;
	
	
	/**
	 * Creates an instance of the UltrasonicNavigationData class.
	 * @param rightMotor
	 * @param leftMotor
	 * @param tile
	 * @param boardSize
	 */
	public UltrasonicNavigationData(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, double tile, double boardSize) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		
		this.tile = tile;
		this.boardSize = boardSize;
	}

	/**
	 * Processes data coming from the ultrasonic poller and performs different processing depending on the
	 * current navigation state of the controller.
	 * In NAVIGATING state, the distance to the closest object in front of the robot is found to avoid potential collisions.
	 * In AVOIDING state, the data processor takes control of the wheels and follows the wall with the assumption that the
	 * ultrasonic sensor is facing the side.
	 * @param distance
	 */
	public void processData(int distance) {
		switch (NavigationController.getNavigationState()) {
	      case NAVIGATING:
	    	  if(distance < OBSTACLE_THRESHOLD) { //an object was detected in front of  the robot, turn sensor towards the wall as the robot rotates
	        	  followingLeftWall = whichDirectionInterruption();
	    		  if (followingLeftWall) {
	    			  navigationController.turnFrontMotor(-290);
	    		  } else {
	    			  navigationController.turnFrontMotor(70);
	    		  }
	        	  navigation.interruptNav(followingLeftWall);
	        	  NavigationController.setNavigationState(NavigationState.AVOIDING);
	          }
	    	  break;
	      case AVOIDING:
	    	  processWallFollowing(distance);
	    	  break;
	    	 default:
	    		 break;
	      }
	}
	
	/**
	 * Processes the input distance to control the motors to follow the wall.
	 * The speed difference between the wheels depends on which side the wall the robot is following is.
	 * To determine this, the method looks at the instance variable -followingLeftWall-
	 * @param distance
	 */
	private void processWallFollowing(int distance) {
		int leftSpeed;
		int rightSpeed;
		if (distance >= 100 && filterControl < FILTER_OUT) {
			// bad value increment the filter value
			// set distance to bandCenter to go straight
			filterControl++;
			this.distance = bandCenter;
		} else if (distance >= 100) {
			// We have repeated large values, so there must actually be nothing
			this.distance = distance;
		} else {
			// distance went below 100: reset filter
			filterControl = 0;
			this.distance = distance;
		}

		corner = false;
		if (this.distance >= 100) {
			leftSpeed = (followingLeftWall) ? CORNER_INNER_SPEED : CORNER_OUTER_SPEED;
			rightSpeed = (!followingLeftWall) ? CORNER_INNER_SPEED : CORNER_OUTER_SPEED;
			leftMotor.setSpeed(leftSpeed);
			rightMotor.setSpeed(rightSpeed);
			rightMotor.forward();
			leftMotor.forward();
			corner = true;
		} else {
			wheelSpeedDifference = Math.abs(ERROR_PROPORTIONALITY * (bandCenter - distance) / 10);
			if (wheelSpeedDifference > 80)
				wheelSpeedDifference = 80;
		}

		if (!corner) {
			if (Math.abs(this.distance - bandCenter) < bandWidth) { // Sweet spot
				leftMotor.setSpeed(MOTOR_SPEED);
				rightMotor.setSpeed(MOTOR_SPEED);
				rightMotor.forward();
				leftMotor.forward();
			} else if (this.distance < bandCenter) { // Too close
				if (this.distance < 8) {
					leftMotor.setSpeed(BACKWARD_SPEED);
					rightMotor.setSpeed(BACKWARD_SPEED);
					rightMotor.backward();
					leftMotor.backward();
				} else if (this.distance < 17) { // if the robot is way too close, it spins
					leftMotor.setSpeed(SPIN_SPEED);
					rightMotor.setSpeed(SPIN_SPEED);
					if (followingLeftWall) {
						rightMotor.backward();
						leftMotor.forward();
					} else {
						rightMotor.forward();
						leftMotor.backward();
					}
				} else {
					leftSpeed = (followingLeftWall) ? MOTOR_SPEED + wheelSpeedDifference : MOTOR_SPEED - wheelSpeedDifference;
					rightSpeed = (!followingLeftWall) ? MOTOR_SPEED + wheelSpeedDifference : MOTOR_SPEED - wheelSpeedDifference;
					leftMotor.setSpeed(leftSpeed);
					rightMotor.setSpeed(rightSpeed);
					rightMotor.forward();
					leftMotor.forward();
				}
			} else { // Too far
				leftSpeed = (!followingLeftWall) ? MOTOR_SPEED + wheelSpeedDifference : MOTOR_SPEED - wheelSpeedDifference;
				rightSpeed = (followingLeftWall) ? MOTOR_SPEED + wheelSpeedDifference : MOTOR_SPEED - wheelSpeedDifference;
				leftMotor.setSpeed(leftSpeed);
				rightMotor.setSpeed(rightSpeed);
				rightMotor.forward();
				leftMotor.forward();
			}
		}
	}
	
	/**
	 * This method returns which direction the robot should turn when its navigation
	 * is interrupted to start wall following. The result is based on its odometer values
	 * Turning right returns true, turning left returns false
	 * @return
	 */
	private boolean whichDirectionInterruption() {
		int currentX = (int) odometer.getX();
		int currentY = (int) odometer.getY();
		int currentTheta = (int) odometer.getThetaDegrees();
		
		int nextHeading = (int) Math.toDegrees(Math.atan2(((boardSize / 2) * tile) - currentX, ((boardSize / 2) * tile) - currentY));
		
		int rightRotation = nextHeading - currentTheta;
		if (rightRotation < 0) {
			rightRotation = rightRotation + 360;
		}
		int leftRotation = currentTheta - nextHeading;
		if (leftRotation < 0) {
			leftRotation = leftRotation + 360;
		}
		return (rightRotation < leftRotation);
	}
	
	/**
	 * Sets the Odometer association of the instance.
	 * @param odometer
	 */
	public void setOdometer(Odometer odometer) {
		this.odometer = odometer;
	}
	
	/**
	 * Sets the Navigation association of the instance.
	 * @param navigation
	 */
	public void setNavigation(Navigation navigation) {
		this.navigation = navigation;
	}
	
	/**
	 * Sets the NavigationController association of the instance.
	 * @param navigationController
	 */
	public void setNavigationController(NavigationController navigationController) {
		this.navigationController = navigationController;
	}
	
}
