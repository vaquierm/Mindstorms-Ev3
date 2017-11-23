/**
 * NavigationController
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.LinkedList;
import java.util.List;

import ca.mcgill.ecse211.capturetheflag.GameParameters.Zone;
import ca.mcgill.ecse211.capturetheflag.UltrasonicPoller.UltrasonicPollingState;
import lejos.hardware.Sound;
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
	 * @param localisation  Association to a Localisation instance
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
	 * @param rectangularPath  True if the navigaion should only travel in the X and Y directions
	 */
	public void runNavigationTask(boolean rectangularPath) {

		if (rectangularPath) {
			try {
				recursivePath(0);
			} catch (StackOverflowError e) {
				Sound.buzz();
				//TODO hardcoding option
			}
		}
		if (objectDetection) {
			ultrasonicPoller.startPolling(UltrasonicPollingState.NAVIGATION);
		}

		while (!coordinateList.isEmpty()) {
			Coordinate point = coordinateList.get(0);
			switch (getNavigationState()) {
			case READY:
				navigation.travelTo(point.x, point.y, true);
				setNavigationState(NavigationState.NAVIGATING);
				break;
			case NAVIGATING:
				if (!rightMotor.isMoving() && !leftMotor.isMoving()) {
					if (!coordinateList.isEmpty())
						coordinateList.remove(0);
					setNavigationState(NavigationState.READY);
				} else {
					if (odometer.getDistanceSinceLastLocalisation() > TILE * RELOCALISATION_CONSTANT) {
						Coordinate closestIntersection = closestIntersection();
						Zone zoneOfIntersection = mapPoint(closestIntersection);
						if (zoneOfIntersection != Zone.RIVER && !closestIntersection.equals(gameParameters.ZC_G) && !closestIntersection.equals(gameParameters.ZC_R)) {
							rightMotor.stop(true);
							leftMotor.stop(); //TODO maybe dont wait
							navigation.travelTo(closestIntersection.x, closestIntersection.y, false);
							localisation.colorLocalisation(false);
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
	public Coordinate closestIntersection() {
		return new Coordinate(localisation.getClosestMultiple(odometer.getX()), localisation.getClosestMultiple(odometer.getY()));
	}

	
	/**
	 * This method takes as input a list of Coordinates and modifies the object such
	 * that the new path is rectangular and avoids physical features on the board
	 * @param i  The index at which the path modification should start
	 * 
	 * @author Michael Vaquier
	 * @author Yujing Duan
	 * 
	 * @return  Returns true if a path was found, false otherwise
	 */
	public boolean recursivePath(int i) throws StackOverflowError {
		if(i == 0) {
			coordinateList.add(0, closestIntersection());
			for(Coordinate coord : coordinateList) {
				if (mapPoint(coord) == Zone.RIVER || coord.equals(gameParameters.ZC_R) || coord.equals(gameParameters.ZC_G)) {
					return false;
				}
			}
		}
		if (i >= 5) {
			boolean repeat = false;
			for (int k = 3; (k <= (i + 1) / 2); k++) {
				if (repeat == true) {
					break;
				}
				int counter = 0;
				for (int j = 0; j < k; j++) {
					if (coordinateList.get(i - j).equals(coordinateList.get(i - j - k))) {
						counter++;
						if (counter == k) {
							repeat = true;
							break;
						}
					}
				}
			}
			if (repeat) {
				return false;
			}
		}
		if(i == coordinateList.size() - 1) {
			removeRedundantPoints();
			return true;
		}
		Coordinate ithCoordinate = coordinateList.get(i);
		Coordinate nextCoordinate = coordinateList.get(i + 1);
		Coordinate bridgeMid = new Coordinate((gameParameters.SV_UR.x + gameParameters.SV_LL.x) / 2, (gameParameters.SH_UR.y + gameParameters.SH_LL.y) / 2);
		if (ithCoordinate.y == nextCoordinate.y) {
			if (mapPoint(ithCoordinate) == mapPoint(nextCoordinate) || mapPoint(nextCoordinate) == Zone.BRIDGE || mapPoint(ithCoordinate) == Zone.BRIDGE) {
				if (obstacleCheck(ithCoordinate, nextCoordinate) && recursivePath(i + 1)) {
					return true;
				} else {
					Coordinate option;
					Coordinate previous = null;
					Coordinate goal;
					int[] currentStatus = {0};
					if(i > 0) {
						previous = coordinateList.get(i - 1);
					}
					if(mapPoint(ithCoordinate) == Zone.BRIDGE) {
						goal = nextCoordinate;
					}
					else if(mapPoint(ithCoordinate) == Zone.GREEN) {
						goal = gameParameters.ZO_G;
					}
					else if(mapPoint(ithCoordinate) == Zone.RED) {
						goal = gameParameters.ZO_R;
					}
					else {
						goal = nextCoordinate;
					}
					for(int j = 0; j < 8; j++) {
						if (mapPoint(ithCoordinate) == Zone.BRIDGE) {
							option = closeShift(ithCoordinate, goal, currentStatus, true, true);
						} else {
							option = closeShift(ithCoordinate, goal, currentStatus, true, false);
						}
						if (mapPoint(option) != Zone.RIVER && !option.equals(previous)) {
							coordinateList.add(i + 1, option);
							if (recursivePath(i + 1)) {
								return true;
							}
							coordinateList.remove(i + 1);
						}
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
		
		else if (ithCoordinate.x == nextCoordinate.x) {
			if (mapPoint(ithCoordinate) == mapPoint(nextCoordinate) || mapPoint(nextCoordinate) == Zone.BRIDGE || mapPoint(ithCoordinate) == Zone.BRIDGE) {
				if (obstacleCheck(ithCoordinate, nextCoordinate) && recursivePath(i + 1)) {
					return true;
				} else {
					Coordinate option;
					Coordinate previous = null;
					Coordinate goal;
					int[] currentStatus = {0};
					if(i > 0) {
						previous = coordinateList.get(i - 1);
					}
					if(mapPoint(ithCoordinate) == Zone.BRIDGE) {
						goal = nextCoordinate;
					}
					else if(mapPoint(ithCoordinate) == Zone.GREEN) {
						goal = gameParameters.ZO_G;
					}
					else if(mapPoint(ithCoordinate) == Zone.RED) {
						goal = gameParameters.ZO_R;
					}
					else {
						goal = nextCoordinate;
					}
					for (int j = 0; j < 8; j++) {
						if (mapPoint(ithCoordinate) == Zone.BRIDGE) {
							option = closeShift(ithCoordinate, goal, currentStatus, true, true);
						} else {
							option = closeShift(ithCoordinate, goal, currentStatus, false, true);
						}
						if (mapPoint(option) != Zone.RIVER && !option.equals(previous)) {
							coordinateList.add(i + 1, option);
							if (recursivePath(i + 1)) {
								return true;
							}
							coordinateList.remove(i + 1);
						}
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
			if (mapPoint(ithCoordinate) == mapPoint(nextCoordinate) || mapPoint(nextCoordinate) == Zone.BRIDGE || mapPoint(ithCoordinate) == Zone.BRIDGE) {
				Coordinate midVH = new Coordinate(ithCoordinate.x, nextCoordinate.y);
				Coordinate midHV = new Coordinate(nextCoordinate.x, ithCoordinate.y);
				Coordinate previous = null;
				if(i > 0) {
					previous = coordinateList.get(i - 1);
				}
				if (!midVH.equals(previous) && mapPoint(midVH) != Zone.RIVER && mapPoint(midVH) != Zone.BRIDGE && (mapPoint(ithCoordinate) != Zone.BRIDGE || mapPoint(midVH) == mapPoint(nextCoordinate)) && obstacleCheck(ithCoordinate, midVH) && obstacleCheck(midVH, nextCoordinate)
						&& !(mapPoint(midHV) != Zone.RIVER && mapPoint(midHV) != Zone.BRIDGE && (mapPoint(ithCoordinate) != Zone.BRIDGE || mapPoint(midHV) == mapPoint(nextCoordinate)) && obstacleCheck(ithCoordinate, midHV) && obstacleCheck(midHV, nextCoordinate))) {
					coordinateList.add(i + 1, midVH);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				if (!midHV.equals(previous) && mapPoint(midHV) != Zone.RIVER && mapPoint(midHV) != Zone.BRIDGE && (mapPoint(ithCoordinate) != Zone.BRIDGE || mapPoint(midHV) == mapPoint(nextCoordinate)) && obstacleCheck(ithCoordinate, midHV) && obstacleCheck(midHV, nextCoordinate)) {
					coordinateList.add(i + 1, midHV);
					if (recursivePath(i + 1)) {
						return true;
					}
					coordinateList.remove(i + 1);
				}
				int[] currentStatus = {0};
				Coordinate option;
				Coordinate goal;
				if(mapPoint(ithCoordinate) == Zone.BRIDGE) {
					goal = nextCoordinate;
				}
				else if(mapPoint(ithCoordinate) == Zone.GREEN) {
					goal = gameParameters.ZO_G;
				}
				else if(mapPoint(ithCoordinate) == Zone.RED) {
					goal = gameParameters.ZO_R;
				}
				else {
					goal = nextCoordinate;
				}
				for (int j = 0; j < 8; j++) {
					option = closeShift(ithCoordinate, goal, currentStatus, true, true);
					if (mapPoint(option) != Zone.RIVER && !option.equals(previous) && obstacleCheck(ithCoordinate, option) && obstacleCheck(option, nextCoordinate)) {
						coordinateList.add(i + 1, option);
						if (recursivePath(i + 1)) {
							return true;
						}
						coordinateList.remove(i + 1);
					}
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
	 * Removes redundant points at the end of generating a path.
	 */
	private void removeRedundantPoints() {
		Coordinate ithCoordinate;
		Coordinate midCoordinate;
		Coordinate nextCoordinate;
		for (int i = 0; i < coordinateList.size() - 2; i++) {
			ithCoordinate = coordinateList.get(i);
			midCoordinate = coordinateList.get(i + 1);
			nextCoordinate = coordinateList.get(i + 2);
			if ((ithCoordinate.x == midCoordinate.x && ithCoordinate.x == nextCoordinate.x) || (ithCoordinate.y == midCoordinate.y && ithCoordinate.y == nextCoordinate.y)) {
				coordinateList.remove(i + 1);
				i--;
			}
		}
	}
	
	/**
	 * This method returns the closest shift to get to the destination
	 * @param ithCoordinate  Current position
	 * @param nextCoordinate  Destination
	 * @param currentStatus  Status of which direction were visited
	 * @param vertical  Allow vertical shifting
	 * @param horizontal  Allow horizontal shifting
	 * @return  The coordinate of the shift
	 */
	private Coordinate closeShift(Coordinate ithCoordinate, Coordinate nextCoordinate, int[] currentStatus, boolean vertical, boolean horizontal) {
		double distance = Integer.MAX_VALUE;
		double x = -1;
		double y = -1;
		int add = -1;
		if (horizontal && (currentStatus[0] & 1) == 0) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x + TILE - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y));
			x = ithCoordinate.x + TILE;
			y = ithCoordinate.y;
			add = 1;
		}
		if (horizontal && (currentStatus[0] & 2) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - TILE - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - TILE - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y));
			x = ithCoordinate.x - TILE;
			y = ithCoordinate.y;
			add = 2;
		}
		if (vertical && (currentStatus[0] & 4) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y + TILE - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y + TILE - nextCoordinate.y));
			x = ithCoordinate.x;
			y = ithCoordinate.y + TILE;
			add = 4;
		}
		if (vertical && (currentStatus[0] & 8) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y - TILE - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y - TILE - nextCoordinate.y));
			x = ithCoordinate.x;
			y = ithCoordinate.y - TILE;
			add = 8;
		}
		if (horizontal && (currentStatus[0] & 16) == 0 && Math.sqrt(Math.abs(ithCoordinate.x + (TILE/2) - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x + (TILE/2) - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y));
			x = ithCoordinate.x + (TILE/2);
			y = ithCoordinate.y;
			add = 16;
		}
		if (horizontal && (currentStatus[0] & 32) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - (TILE/2) - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - (TILE/2) - nextCoordinate.x) + Math.abs(ithCoordinate.y - nextCoordinate.y));
			x = ithCoordinate.x - (TILE/2);
			y = ithCoordinate.y;
			add = 32;
		}
		if (vertical && (currentStatus[0] & 64) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y + (TILE/2) - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y + (TILE/2) - nextCoordinate.y));
			x = ithCoordinate.x;
			y = ithCoordinate.y + (TILE/2);
			add = 64;
		}
		if (vertical && (currentStatus[0] & 128) == 0 && Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y - (TILE/2) - nextCoordinate.y)) < distance) {
			distance = Math.sqrt(Math.abs(ithCoordinate.x - nextCoordinate.x) + Math.abs(ithCoordinate.y - (TILE/2) - nextCoordinate.y));
			x = ithCoordinate.x;
			y = ithCoordinate.y - (TILE/2);
			add = 128;
		}
		if(add > 0)
			currentStatus[0] += add;
		return new Coordinate(x, y);
	}
	
	
	
	
	
	/**
	 * This method takes as an input a coordinate and returns the region in which this point fits in, by looking at the gameParapeters
	 * association that the instance has.
	 * @param node  Coordinate checked for which zone it is in
	 * @return  The zone in which the Coordinate falls in
	 */
	public Zone mapPoint(Coordinate node)
	{
		if(node.x > gameParameters.Green_LL.x && node.x < gameParameters.Green_UR.x && node.y > gameParameters.Green_LL.y && node.y < gameParameters.Green_UR.y)
		{
			return Zone.GREEN;
		}
		else if(node.x > gameParameters.Red_LL.x && node.x < gameParameters.Red_UR.x && node.y > gameParameters.Red_LL.y && node.y < gameParameters.Red_UR.y)
		{
			return Zone.RED;
		}
		else if((node.x > gameParameters.SV_LL.x && node.x < gameParameters.SV_UR.x && node.y > gameParameters.SV_LL.y && node.y < gameParameters.SV_UR.y)
				||(node.x > gameParameters.SH_LL.x && node.x < gameParameters.SH_UR.x && node.y > gameParameters.SH_LL.y && node.y < gameParameters.SH_UR.y))
		{
			return Zone.BRIDGE;
		}
		else if (((node.x >= gameParameters.Green_LL.x && node.x <= gameParameters.Green_UR.x && node.y >= gameParameters.Green_LL.y && node.y <= gameParameters.Green_UR.y)
				|| (node.x >= gameParameters.Red_LL.x && node.x <= gameParameters.Red_UR.x && node.y >= gameParameters.Red_LL.y && node.y <= gameParameters.Red_UR.y))
				&& ((node.x >= gameParameters.SV_LL.x && node.x <= gameParameters.SV_UR.x && node.y >= gameParameters.SV_LL.y && node.y <= gameParameters.SV_UR.y)
				||(node.x >= gameParameters.SH_LL.x && node.x <= gameParameters.SH_UR.x && node.y >= gameParameters.SH_LL.y && node.y <= gameParameters.SH_UR.y))
				&& (!(node.x == gameParameters.SH_LL.x && node.y == gameParameters.SH_LL.y) && !(node.x == gameParameters.SH_UR.x && node.y == gameParameters.SH_UR.y) && !(node.x == gameParameters.SV_LL.x && node.y == gameParameters.SV_LL.y) && !(node.x == gameParameters.SV_UR.x && node.y == gameParameters.SV_UR.y))
				&& (!(node.x == gameParameters.SH_LL.x && node.y == gameParameters.SH_UR.y) && !(node.x == gameParameters.SH_UR.x && node.y == gameParameters.SH_LL.y) && !(node.x == gameParameters.SV_LL.x && node.y == gameParameters.SV_UR.y) && !(node.x == gameParameters.SV_UR.x && node.y == gameParameters.SV_LL.y))) {
			return Zone.BRIDGE;
		}
		else {
			return Zone.RIVER;
		}
	}
	
	
	/**
	 * This method takes as an input a coordinate and returns the region in which this point fits in, by looking at the gameParapeters
	 * association that the instance has.
	 * @param x  The X coordinate of the node
	 * @param y  The Y coordinate of the node
	 * @return  The zone in which the Coordinate falls in
	 */
	public Zone mapPoint(double x, double y)
	{
		if(x > gameParameters.Green_LL.x + 1 && x < gameParameters.Green_UR.x - 1 && y > gameParameters.Green_LL.y + 1 && y < gameParameters.Green_UR.y - 1)
		{
			return Zone.GREEN;
		}
		else if(x > gameParameters.Red_LL.x + 1 && x < gameParameters.Red_UR.x - 1 && y > gameParameters.Red_LL.y + 1 && y < gameParameters.Red_UR.y - 1)
		{
			return Zone.RED;
		}
		else if((x > gameParameters.SV_LL.x + 1 && x < gameParameters.SV_UR.x - 1 && y > gameParameters.SV_LL.y + 1 && y < gameParameters.SV_UR.y - 1)
				||(x > gameParameters.SH_LL.x + 1 && x < gameParameters.SH_UR.x - 1 && y > gameParameters.SH_LL.y + 1 && y < gameParameters.SH_UR.y - 1))
		{
			return Zone.BRIDGE;
		}
		else {
			return Zone.RIVER;
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
			if (gameParameters.ZC_G.x == gameParameters.ZC_R.x) {
				if ((a.y < gameParameters.ZC_R.y && a.y > gameParameters.ZC_G.y) || (a.y > gameParameters.ZC_R.y && a.y < gameParameters.ZC_G.y)) {
					if (((a.x <= gameParameters.ZC_G.x && b.x >= gameParameters.ZC_G.x) || (a.x >= gameParameters.ZC_G.x && b.x <= gameParameters.ZC_G.x))) {
						return false;
					}
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
			if (gameParameters.ZC_G.y == gameParameters.ZC_R.y) {
				if ((a.x < gameParameters.ZC_R.x && a.x > gameParameters.ZC_G.x) && (a.x > gameParameters.ZC_R.x && a.x < gameParameters.ZC_G.x)) {
					if (((a.y <= gameParameters.ZC_G.y && b.y >= gameParameters.ZC_G.y) || (a.y >= gameParameters.ZC_G.y && b.y <= gameParameters.ZC_G.y))) {
						return false;
					}
				}
			}
		}
		if (gameParameters.ZC_G.x != gameParameters.ZC_R.x && gameParameters.ZC_G.y != gameParameters.ZC_R.y) {
			double slope;
			double yIntercept;
			double lineXIntercept;
			double lineYIntercept;
			if (gameParameters.ZC_R.x > gameParameters.ZC_G.x) {
				slope = (gameParameters.ZC_R.y - gameParameters.ZC_G.y) / (gameParameters.ZC_R.x - gameParameters.ZC_G.x);				
			} else {
				slope = (gameParameters.ZC_G.y - gameParameters.ZC_R.y) / (gameParameters.ZC_G.x - gameParameters.ZC_R.x);
			}
			yIntercept = (slope * gameParameters.ZC_G.x) - gameParameters.ZC_G.y;
			if (a.x == b.x && ((a.x <= gameParameters.ZC_G.x && a.x >= gameParameters.ZC_R.x) || (a.x >= gameParameters.ZC_G.x && a.x <= gameParameters.ZC_R.x))) {
				lineYIntercept = (slope * a.x) + yIntercept;
				lineXIntercept = (lineYIntercept - yIntercept) / slope;
				if ((lineYIntercept <= a.y && lineYIntercept >= b.y) || (lineYIntercept >= a.y && lineYIntercept <= b.y)) {
					return false;
				}
			} else if (a.y == b.y && ((a.y <= gameParameters.ZC_G.y && a.y >= gameParameters.ZC_R.y) || (a.y >= gameParameters.ZC_G.y && a.y <= gameParameters.ZC_R.y))) {
				lineXIntercept = (a.y - yIntercept) / slope;
				lineYIntercept = (slope * lineXIntercept) + yIntercept;
				if ((lineXIntercept <= a.x && lineXIntercept >= b.x) || (lineXIntercept >= a.x && lineXIntercept <= b.x)) {
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