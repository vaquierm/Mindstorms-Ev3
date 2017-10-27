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

	// Associations
	Odometer odometer;
	Navigation navigation;

	// Motors
	EV3LargeRegulatedMotor rightMotor;
	EV3LargeRegulatedMotor leftMotor;

	// Robot constants

	public enum NavigationState {
		NAVIGATING, AVOIDING, READY
	}

	public NavigationController(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, Odometer odometer,
			Navigation navigation) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;

		this.odometer = odometer;
		this.navigation = navigation;
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

	private void changeToRectangularPath(List<Coordinate> wayPoints) {
		// TODO Bill fill this to travel in rectangular paths rater than
		// diagonals and avoid physical features
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