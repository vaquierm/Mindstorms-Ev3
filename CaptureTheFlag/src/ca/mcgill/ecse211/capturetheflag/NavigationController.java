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
	
	private static int RELOCALISATION_CONSTANT = 4;
	private final GameParameters gameParameters;
	private final double TILE;

	// Associations
	private Odometer odometer;
	private Navigation navigation;
	private Localisation localisation;
	
	//Poller
	private UltrasonicPoller ultrasonicPoller;

	// Motors
	private EV3LargeRegulatedMotor rightMotor;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3MediumRegulatedMotor frontMotor;
	
	/**
	 * Represents the tolerated error in cm.
	 */
	private static final double NAVIGATION_HEADING_ERROR_TOLERENCE = 3;

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
	 * @param TILE  The  tile length of the game board
	 */
	public NavigationController(EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, EV3MediumRegulatedMotor frontMotor,
			Odometer odometer, Navigation navigation, Localisation localisation, UltrasonicPoller ultrasonicPoller, GameParameters gameParameters, double TILE) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.frontMotor = frontMotor;
		frontMotor.setSpeed(30);
		frontMotor.resetTachoCount();

		this.odometer = odometer;
		this.navigation = navigation;
		this.localisation = localisation;
		
		this.ultrasonicPoller = ultrasonicPoller;
		
		this.gameParameters = gameParameters;
		this.TILE = TILE;
		
		ultrasonicPoller.getUltrasonicNavigationData().setNavigation(navigation);
		ultrasonicPoller.getUltrasonicNavigationData().setNavigationController(this);
		ultrasonicPoller.getUltrasonicNavigationData().setOdometer(odometer);
	}

	/**
	 * Runs the navigation task, and constantly checks if the heading at which the robot is traveling to
	 * is consistent with the next wayPoint
	 */
	public void runNavigationTask(boolean rectangularPath) {

		if (rectangularPath) {
			recursivePath(0);
		}
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
					double nextHeadingError = odometer.getThetaDegrees() - Math.toDegrees(Math.atan2(point.x - x, point.y - y));
					if (nextHeadingError < 0)
						nextHeadingError += 360;
					double distance = Math.sqrt(Math.pow(x - point.x, 2) + Math.pow(y - point.y, 2));
					if (distance * nextHeadingError < NAVIGATION_HEADING_ERROR_TOLERENCE) {
						rightMotor.stop(true);
						leftMotor.stop();
						navigation.travelTo(point.x, point.y, true);
					} else if (odometer.getDistanceSinceLastLocalisation() > TILE * RELOCALISATION_CONSTANT) {
						Coordinate closestIntersection = closestIntersection();
						String zoneOfIntersection = mapPoint(closestIntersection);
						if (!zoneOfIntersection.equals("river") && !zoneOfIntersection.equals("bridge")) {
							rightMotor.stop(true);
							leftMotor.stop();
							navigation.travelTo(closestIntersection.x, closestIntersection.y, false);
							localisation.colorLocalisation();
							if(rectangularPath) {
								recursivePath(0);
							}
							setNavigationState(NavigationState.READY);
						}
					}
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
	 * Returns the coordinate of the closest intersection from the robot from its odometer values.
	 * @return  The closest intersection Coordinate from the robot
	 */
	private Coordinate closestIntersection() {
		return new Coordinate(localisation.getClosestMultiple(odometer.getX()), localisation.getClosestMultiple(odometer.getY()));
	}

	
	/**
	 * This method takes as input a list of Coordinates and modifies the object such
	 * that the new path is rectangular and avoids physical features on the board
	 * @param i  The index at which the path modification should start
	 * 
	 * @author Michael Vaquier
	 * @author Yujing Duan
	 */
	public boolean recursivePath(int i) {
		if(i == 0) {
			coordinateList.add(0, closestIntersection());
		}
		if(i == coordinateList.size() - 1) {
			return true;
		}
		Coordinate ithCoordinate = coordinateList.get(i);
		Coordinate nextCoordinate = coordinateList.get(i + 1);
		Coordinate bridgeMid = new Coordinate((gameParameters.SV_UR.x + gameParameters.SV_LL.x) / 2, (gameParameters.SH_UR.y + gameParameters.SH_LL.y) / 2);
		if (ithCoordinate.y == nextCoordinate.y && obstacleCheck(ithCoordinate, nextCoordinate)) {
			if (mapPoint(ithCoordinate).equals(mapPoint(nextCoordinate)) || mapPoint(nextCoordinate).equals("bridge") || mapPoint(ithCoordinate).equals("bridge")) {
				if (recursivePath(i + 1)) {
					return true;
				} else {
					Coordinate option1 = new Coordinate(ithCoordinate.x, ithCoordinate.y + TILE);
					Coordinate option2 = new Coordinate(ithCoordinate.x, ithCoordinate.y - TILE);
					Coordinate previous = null;
					if(i > 0) {
						previous = coordinateList.get(i - 1);
					}
					if (!mapPoint(option1).equals("river") && !mapPoint(option1).equals("bridge") && !option1.equals(previous)) {
						coordinateList.add(i + 1, option1);
						if (recursivePath(i + 1)) {
							return true;
						}
						coordinateList.remove(i + 1);
					}
					if (!mapPoint(option2).equals("river") && !mapPoint(option2).equals("bridge") && !option2.equals(previous)) {
						coordinateList.add(i + 1, option2);
						if (recursivePath(i + 1)) {
							return true;
						}
						coordinateList.remove(i + 1);
					}
					return false;
				}
			}
			else {
				coordinateList.add(i + 1, bridgeMid);
				if (recursivePath(i)) {
					return true;
				}
				coordinateList.remove(i + 1);
				return false;
			}
		}
		
		else if (ithCoordinate.x == nextCoordinate.x && obstacleCheck(ithCoordinate, nextCoordinate)) {
			if (mapPoint(ithCoordinate).equals(mapPoint(nextCoordinate)) || mapPoint(nextCoordinate).equals("bridge") || mapPoint(ithCoordinate).equals("bridge")) {
				if (recursivePath(i + 1)) {
					return true;
				} else {
					Coordinate option1 = new Coordinate(ithCoordinate.x + TILE, ithCoordinate.y);
					Coordinate option2 = new Coordinate(ithCoordinate.x - TILE, ithCoordinate.y);
					Coordinate previous = null;
					if(i > 0) {
						previous = coordinateList.get(i - 1);
					}
					if (!mapPoint(option1).equals("river") && !mapPoint(option1).equals("bridge") && !option1.equals(previous)) {
						coordinateList.add(i + 1, option1);
						if (recursivePath(i + 1)) {
							return true;
						}
						coordinateList.remove(i + 1);
					}
					if (!mapPoint(option2).equals("river") && !mapPoint(option2).equals("bridge") && !option2.equals(previous)) {
						coordinateList.add(i + 1, option2);
						if (recursivePath(i + 1)) {
							return true;
						}
						coordinateList.remove(i + 1);
					}
					return false;
				}
			}
			else {
				coordinateList.add(i + 1, bridgeMid);
				if (recursivePath(i)) {
					return true;
				}
				coordinateList.remove(i + 1);
				return false;
			}
		}
		
		else {
			if (mapPoint(ithCoordinate).equals(mapPoint(nextCoordinate)) || mapPoint(nextCoordinate).equals("bridge") || mapPoint(ithCoordinate).equals("bridge")) {
				Coordinate midVH = new Coordinate(ithCoordinate.x, nextCoordinate.y);
				Coordinate midHV = new Coordinate(nextCoordinate.x, ithCoordinate.y);
				if (!mapPoint(midVH).equals("river") && !mapPoint(midVH).equals("bridge") && obstacleCheck(ithCoordinate, midVH) && obstacleCheck(midVH, nextCoordinate)) {
					coordinateList.add(i + 1, midVH);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				if (!mapPoint(midHV).equals("river") && !mapPoint(midHV).equals("bridge") && obstacleCheck(ithCoordinate, midHV) && obstacleCheck(midHV, nextCoordinate)) {
					coordinateList.add(i + 1, midHV);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				Coordinate option = new Coordinate(ithCoordinate.x + TILE, ithCoordinate.y);
				Coordinate previous = null;
				if(i > 0) {
					previous = coordinateList.get(i - 1);
				}
				if (!mapPoint(option).equals("river") && !mapPoint(option).equals("bridge") && !option.equals(previous) && obstacleCheck(ithCoordinate, option) && obstacleCheck(option, nextCoordinate)) {
					coordinateList.add(i + 1, option);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				option = new Coordinate(ithCoordinate.x - TILE, ithCoordinate.y);
				if (!mapPoint(option).equals("river") && !mapPoint(option).equals("bridge") && !option.equals(previous) && obstacleCheck(ithCoordinate, option) && obstacleCheck(option, nextCoordinate)) {
					coordinateList.add(i + 1, option);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				option = new Coordinate(ithCoordinate.x, ithCoordinate.y + TILE);
				if (!mapPoint(option).equals("river") && !mapPoint(option).equals("bridge") && !option.equals(previous) && obstacleCheck(ithCoordinate, option) && obstacleCheck(option, nextCoordinate)) {
					coordinateList.add(i + 1, option);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				option = new Coordinate(ithCoordinate.x, ithCoordinate.y - TILE);
				if (!mapPoint(option).equals("river") && !mapPoint(option).equals("bridge") && !option.equals(previous) && obstacleCheck(ithCoordinate, option) && obstacleCheck(option, nextCoordinate)) {
					coordinateList.add(i + 1, option);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				return false;
			}
			else {
				coordinateList.add(i + 1, bridgeMid);
				if (recursivePath(i)) {
					return true;
				}
				coordinateList.remove(i + 1);
				return false;
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
			return "green";
		}
		else if(node.x > gameParameters.Red_LL.x && node.x < gameParameters.Red_UR.x && node.y > gameParameters.Red_LL.y && node.y < gameParameters.Red_UR.y)
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
	 * Checks if there is any obstacles in the way of two coordinate on the same line
	 * @param a  First Coordinate
	 * @param b  Second Coordinate
	 * @return  True if no obstacles are present
	 */
	private boolean obstacleCheck(Coordinate a, Coordinate b) {
		if (a.y == b.y) {
			if (a.y == gameParameters.ZC_R.y) {
				if ((a.x <= gameParameters.ZC_R.x && b.x >= gameParameters.ZC_R.x) || (a.x >= gameParameters.ZC_R.x && b.x <= gameParameters.ZC_R.x)) {
					return false;
				}
			}
			if (a.y == gameParameters.ZC_G.y) {
				if (((a.x <= gameParameters.ZC_G.x && b.x >= gameParameters.ZC_G.x) || (a.x >= gameParameters.ZC_G.x && b.x <= gameParameters.ZC_G.x))) {
					return false;
				}
			}
		}
		if (a.x == b.x) {
			if (a.x == gameParameters.ZC_R.x) {
				if ((a.y <= gameParameters.ZC_R.y && b.y >= gameParameters.ZC_R.y) || (a.y >= gameParameters.ZC_R.y && b.y <= gameParameters.ZC_R.y)) {
					return false;
				}
			}
			if (a.x == gameParameters.ZC_G.x) {
				if (((a.y <= gameParameters.ZC_G.y && b.y >= gameParameters.ZC_G.y) || (a.y >= gameParameters.ZC_G.y && b.y <= gameParameters.ZC_G.y))) {
					return false;
				}
			}
		}
		return true;
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
	 * Adds a way point at the end of the current list.
	 * @param x  The X value of the wayPoint to be added
	 * @param y  The Y value of the wayPoint to be added
	 */
	public void addWayPoint(double x, double y) {
		coordinateList.add(new Coordinate(x, y));
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