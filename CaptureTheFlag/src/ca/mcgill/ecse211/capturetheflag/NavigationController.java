/**
 * NavigationController
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.List;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * 
 * NavigationController allows the robot to switch between navigating point to
 * point using Navigation and wall following around an obstacle to avoid it
 * using PController
 * 
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class NavigationController implements Runnable {

	// Locks used for threading
	private static Object stateLock = new Object();

	private List<Coordinate> coordinateList;
	private final boolean objectDetection = false;
	private static NavigationState state = NavigationState.READY;
	
	private final GameParameters gameParameters;

	// Associations
	Odometer odometer;
	Navigation navigation;

	// Motors
	EV3LargeRegulatedMotor rightMotor;
	EV3LargeRegulatedMotor leftMotor;


	public enum NavigationState {
		NAVIGATING, AVOIDING, READY
	}

	public NavigationController(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, Odometer odometer,
			Navigation navigation, GameParameters gameParameters) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;

		this.odometer = odometer;
		this.navigation = navigation;
		
		this.gameParameters = gameParameters;
	}

	public void run() {

		if (objectDetection) {
			objectAvoidanceUsPoller.resumeThread();
		}

		while (coordinateList.size() > 0) {
			Coordinate point = coordinateList.get(0);
			switch (getNavigationState()) {
			case READY:
				navigation.travelTo(point.x, point.y, true);
				setNavigationState(NavigationState.NAVIGATING);
				break;
			case NAVIGATING:

				double x = odometer.getX();
				double y = odometer.getY();

				if (!rightMotor.isMoving() && !leftMotor.isMoving()
						&& Math.sqrt(Math.pow(x - point.x, 2) + Math.pow(y - point.y, 2)) < 5) { 
					coordinateList.remove(0);
					setNavigationState(NavigationState.READY);
				}

				break;
			case AVOIDING:
				int interrupted = (int) navigation.getInterruptedTheta();
				int current = (int) odometer.getThetaDegrees();
				int difference = Math.abs(interrupted - current);
				// System.out.println(difference);
				if (difference < 195 && difference > 165) { // when the robot is pointing to the opposite direction after it was interrupted, we can start navigating again.
					setNavigationState(NavigationState.READY);
					objectAvoidanceUsPoller.usSensorStraight();
				}
				break;
			default:
				break;
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {

			}
		}
		objectAvoidanceUsPoller.stopPolling();
	}

	
	/**
	 * This method takes as input a list of Coordinates and modifies the object such
	 * that the new path is rectangular and avoids physical features on the board
	 * @param wayPoints
	 */
	private void changeToRectangularPath(List<Coordinate> wayPoints) {
		// TODO
		/**
		 * Look at the GameParameters class, all the parameters are there, this class has a reference to an instance of GameParameters
		 * Use it to determine what path we cant use. You also need to use the odometer to get a rough idea of the current position of the robot.
		 * 
		 */
		
	}

	public static NavigationState getNavigationState() {
		synchronized (stateLock) {
			return state;
		}
	}

	public static void setNavigationState(NavigationState navState) {
		synchronized (stateLock) {
			state = navState;
		}
	}

}