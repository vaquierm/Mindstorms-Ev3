/**
 * NavigationController
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.LinkedList;
import java.util.List;

import ca.mcgill.ecse211.capturetheflag.UltrasonicPoller.UltrasonicPollingState;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;

/**
 * 
 * NavigationController allows the robot to switch between navigating point to
 * point using Navigation and wall following around an obstacle to avoid it
 * using PController logic.
 * A state machine is used to determine the current state of the navigation and adjusts its behavior accordingly to its state.
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 * @author Zeyu Chen
 */
public class NavigationController {

	// Locks used for threading
	private static Object stateLock = new Object();

	private List<Coordinate> coordinateList = new LinkedList<Coordinate>();
	private volatile boolean objectDetection = false;
	private static NavigationState state = NavigationState.READY;
	
	private final GameParameters gameParameters;

	// Associations
	private Odometer odometer;
	private Navigation navigation;
	
	//Poller
	private UltrasonicPoller ultrasonicPoller;

	// Motors
	private EV3LargeRegulatedMotor rightMotor;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3MediumRegulatedMotor frontMotor;
	
	/**
	 * Represents the tolerated error in heading as a percentage of the distance left to travel.
	 */
	private static final double NAVIGATION_HEADING_ERROR_TOLERENCE = 0.05;

	/**
	 * This enumeration defines the states in which the navigation controller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum NavigationState {
		NAVIGATING, AVOIDING, READY
	}

	/**
	 * Creates a NavigationController object.
	 * @param rightMotor  Reference to the right motor
	 * @param leftMotor  Reference to the left motor
	 * @param frontMotor  Reference to the front motor
	 * @param odometer  Association to an Odometer instance
	 * @param navigation  Association to a Navigation instance
	 * @param ultrasonicPoller  Association to an UltrasonicPoller
	 * @param gameParameters  Game parameters for this round
	 */
	public NavigationController(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, EV3MediumRegulatedMotor frontMotor,
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
		
		ultrasonicPoller.getUltrasonicNavigationData().setNavigation(navigation);
		ultrasonicPoller.getUltrasonicNavigationData().setNavigationController(this);
		ultrasonicPoller.getUltrasonicNavigationData().setOdometer(odometer);
	}

	/**
	 * Runs the navigation task, and constantly checks if the heading at which the robot is traveling to
	 * is consistent with the next wayPoint
	 */
	public void runNavigationTask() {

		if (objectDetection) {
			ultrasonicPoller.startPolling(UltrasonicPollingState.NAVIGATION);
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

				if (!rightMotor.isMoving() && !leftMotor.isMoving()) { 
					coordinateList.remove(0);
					setNavigationState(NavigationState.READY);
				} else {
					/*double nextHeadingRadError = odometer.getTheta() - Math.atan2(point.x - x, point.y - y);
					if (nextHeadingRadError < 0)
						nextHeadingRadError += 2 * Math.PI;
					double distance = Math.sqrt(Math.pow(x - point.x, 2) + Math.pow(y - point.y, 2));
					System.out.println("distanca" + distance);
					System.out.println("headingerror" +nextHeadingRadError);
					if (distance * nextHeadingRadError < distance * NAVIGATION_HEADING_ERROR_TOLERENCE) {
						rightMotor.stop(true);
						leftMotor.stop();
						navigation.travelTo(point.x, point.y, true);
					}*/
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
	 * @param wayPoints List of Coordinates to be modified
	 */
	private void changeToRectangularPath(List<Coordinate> wayPoints) {
		double midx = (gameParameters.SV_UR.x + gameParameters.SV_LL.x) / 2;
		double midy = (gameParameters.SH_UR.x + gameParameters.SH_LL.x) / 2;
		Coordinate bridgemid = new Coordinate(midx, midy);
		
		for(int i = 0 ; i < (wayPoints.size() - 1) ; i++)
		{
			if(wayPoints.get(i).x != wayPoints.get(i+1).x && wayPoints.get(i).y != wayPoints.get(i+1).y)
			{
				Coordinate midVH = new Coordinate(wayPoints.get(i).x, wayPoints.get(i+1).y);
				Coordinate midHV = new Coordinate(wayPoints.get(i+1).x, wayPoints.get(i).y);
				
                if(!mapPoint(midHV).equals("river"))
                {
                	if(mapPoint(wayPoints.get(i)).equals(mapPoint(midHV)))
                	{
                		wayPoints.add(i+1, midHV);
                	}
                	else
                	{
                		if(mapPoint(wayPoints.get(i)).equals("bridge"))
                		{
                			wayPoints.add(i+1, midHV);
                		}    	
                		else{		
                			wayPoints.add(i+1,bridgemid);
                			i = i - 1;
                		}             		
                	}
                }
                
                else if(mapPoint(midHV).equals("river") && !mapPoint(midVH).equals("river"))
                {
                	if(mapPoint(wayPoints.get(i)).equals(mapPoint(midVH)))
                	{
                		wayPoints.add(i+1, midVH);
                	}
                	else
                	{
                		if(mapPoint(wayPoints.get(i)).equals("bridge"))
                		{
                			wayPoints.add(i+1, midVH);
                		}    	
                		else{		
                			wayPoints.add(i+1, bridgemid);
                			i = i - 1;
                		}             		
                	}
                }
                
                else 
                {
                	wayPoints.add(i+1, bridgemid);
                	i = i - 1;
				}
                
			}
			else
			{
				if(!mapPoint(wayPoints.get(i)).equals(mapPoint(wayPoints.get(i+1))))
				{
					wayPoints.add(i+1, bridgemid);
				}
			}

		}
	}
	
	/**
	 * This method takes as an input a coordinate and returns the region in which this point fits in, by looking at the gameParapeters
	 * association that the instance has.
	 * @param node  Coordinate checked for which zone it is in
	 * @return  The zone in which the Coordinate falls in
	 */
	private String mapPoint(Coordinate node)
	{
		if(node.x > gameParameters.Green_LL.x && node.x < gameParameters.Green_UR.x && node.y > gameParameters.Green_LL.y && node.y < gameParameters.Green_UR.y)
		{
			return "Green";
		}
		else if(node.x > gameParameters.Red_LL.x && node.x < gameParameters.Red_UR.x && node.y > gameParameters.Red_LL.y && node.y < gameParameters.Red_LL.y)
		{
			return "red";
		}
		else if((node.x > gameParameters.SV_LL.x && node.x < gameParameters.SV_UR.x && node.y > gameParameters.SV_LL.y && node.y < gameParameters.SV_UR.y)
				||(node.x > gameParameters.SH_LL.x && node.x < gameParameters.SH_UR.x && node.y > gameParameters.SH_LL.y && node.y < gameParameters.SH_UR.y))
		{
			return "bridge";
		}
		else {
			return "river";
		}
	}
	
	/**
	 * Sets a new list of wayPoints to travel to.
	 * @param coordinates  New list of wayPoits
	 */
	public void setCoordinateList(List<Coordinate> coordinates) {
		this.coordinateList = coordinates;
	}
	
	/**
	 * Adds a way point at the end of the current list.
	 * @param newPoint  Coordinate to be added to the list
	 */
	public void addWayPoint(Coordinate newPoint) {
		coordinateList.add(newPoint);
	}
	
	/**
	 * This method rotates the front motor where the sensors are attached to a specific angle
	 * and returns the thread immediately
	 * @param angle  Angle to which the front motor has to be turned
	 */
	public void turnFrontMotor(int angle) {
		frontMotor.rotateTo(angle, true);
	}
	
	/**
	 * This method should be called before running the navigation to determine of the system should be looking for objects to avoid or not.
	 * @param b  Navigation will avoid objects if true
	 */
	public void setObjectAvoidance(boolean b) {
		objectDetection = b;
	}

	/**
	 * Returns the current navigation state of the robot.
	 * @return  The current navigation state
	 */
	public static NavigationState getNavigationState() {
		synchronized (stateLock) {
			return state;
		}
	}
	
	/**
	 * Sets the navigationState of the robot.
	 * @param navState  The new navigation state to be set
	 */
	public static void setNavigationState(NavigationState navState) {
		synchronized (stateLock) {
			state = navState;
		}
	}

}