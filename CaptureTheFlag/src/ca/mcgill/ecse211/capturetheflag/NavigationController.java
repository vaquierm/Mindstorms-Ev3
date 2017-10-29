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
	private volatile boolean objectDetection = false;
	private static NavigationState state = NavigationState.READY;
	
	private final GameParameters gameParameters;

	// Associations
	Odometer odometer;
	Navigation navigation;
	
	//Poller
	UltrasonicPoller ultrasonicPoller;

	// Motors
	EV3LargeRegulatedMotor rightMotor;
	EV3LargeRegulatedMotor leftMotor;
	EV3LargeRegulatedMotor frontMotor;


	public enum NavigationState {
		NAVIGATING, AVOIDING, READY
	}

	public NavigationController(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor frontMotor,
			Odometer odometer, Navigation navigation, UltrasonicPoller ultrasonicPoller, GameParameters gameParameters) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.frontMotor = frontMotor;
		frontMotor.setSpeed(30);
		frontMotor.resetTachoCount();

		this.odometer = odometer;
		this.navigation = navigation;
		this.ultrasonicPoller = ultrasonicPoller;
		
		this.gameParameters = gameParameters;
	}

	public void run() {

		if (objectDetection) {
			ultrasonicPoller.startPolling();
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
				if (difference < 195 && difference > 165) { // when the robot is pointing to the opposite direction after it was interrupted, we can start navigating again.
					setNavigationState(NavigationState.READY);
					frontMotor.rotateTo(0, true);
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
		ultrasonicPoller.stopPolling();
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
	
	/**
	 * This method rotates the front motor where the sensors are attached to a specific angle
	 * and returns the thread immediately
	 * @param angle
	 */
	public void turnFrontMotor(int angle) {
		frontMotor.rotateTo(angle, true);
	}
	
	/**
	 * This method should be called before running the navigation to determine of the system should be looking for objects to avoid or not.
	 * @param b
	 */
	public void setObjectAvoidance(boolean b) {
		objectDetection = b;
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