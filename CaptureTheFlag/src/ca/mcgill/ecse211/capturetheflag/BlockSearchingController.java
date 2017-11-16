/**
 * BlockSearchingController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import ca.mcgill.ecse211.capturetheflag.ColorPoller.ColorPollingState;
import ca.mcgill.ecse211.capturetheflag.GameParameters.Zone;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * BlockSearchingController is the subtask involving finding a block of a specific
 * Color in a specified search area
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 * @author Zeyu Chen
 */

public class BlockSearchingController {
	
	private static final int SHUFFLE_SPEED = 200;
	private static final int ROTATION = 180;
	
	private final GameParameters gameParameters;
	
	//Associations
	private final ColorPoller colorPoller;
	
	//Thread variables
	private volatile boolean paused = false;
	private Object pauseLock = new Object();
	
	private final double tile;
	private Coordinate lowerLeft;
	private Coordinate upperRight;
	private Coordinate nextSweep;
	int xIncrement;
	int yIncrement;
	
	//Motors
	private boolean whitchMotor = true;
	private final EV3LargeRegulatedMotor leftMotor;
	private final EV3LargeRegulatedMotor rightMotor;
	
	//Associations
	private final Odometer odometer;
	private final Navigation navigation;
	private final Localisation localisation;
	private final NavigationController navigationController;
	
	private Object stateLock = new Object();
	private BlockSearchingState blockSearchingState = BlockSearchingState.SEARCHING;
	private boolean searchMethod;
	
	/**
	 * this enumeration delimitates the states in which the block searching controller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum BlockSearchingState {SEARCHING, FOUND, ABANDON}
	
	
	public BlockSearchingController(Odometer odometer, NavigationController navigationController, Navigation navigation, Localisation localisation, EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, ColorPoller colorPoller, GameParameters gameParameters, double tile, int teamNumber) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		
		this.odometer = odometer;
		this.navigation = navigation;
		this.localisation = localisation;
		this.navigationController = navigationController;
		
		this.colorPoller = colorPoller;
		this.gameParameters = gameParameters;
		
		this.tile = tile;
		
		determineSearchMethod(teamNumber);
		
		colorPoller.getBlockSearchingData().setBlockSeachingController(this);
	}
	
	/**
	 * This method runs the subtask involving the block searching component of the
	 * competition.
	 */
	public void runBlockSearchingTask() {
		nextSweep = closestValidPoint();
		navigationController.addWayPoint(nextSweep);
		navigationController.runNavigationTask(true);
		setBlockSearchingState(BlockSearchingState.SEARCHING);
		colorPoller.startPolling(ColorPollingState.BLOCK_SEARCHING);
		spinMethod();
		/*if (searchMethod) {
			contourMethod();
		} else {
			spinMethod();
		}*/
		colorPoller.stopPolling();
	}
	
	/**
	 * Search method where the robot first navigates around the search area with a color sensor towards the center, then navigating to the center to perform a sweep.
	 */
	private void contourMethod() {
		
	}
	
	/**
	 * Search method where the robot navigates to every intersection of the search area and performs a localisation sweep combination.
	 */
	private void spinMethod() {
		while (true) {
			switch (getBlockSearchingState()) {
			case SEARCHING:
				if (!rightMotor.isMoving() && !leftMotor.isMoving()) {
					localisation.colorLocalisation();
					if (getBlockSearchingState() == BlockSearchingState.SEARCHING) {
						incrementNextSweep();
						if (isInSearchArea(nextSweep)) {
							navigation.travelTo(nextSweep.x, nextSweep.y, true);
						} else {
							setBlockSearchingState(BlockSearchingState.ABANDON);
						}
					}
				}
				
				break;
			case FOUND:
				Sound.twoBeeps(); //TODO replace to fun sound
				rightMotor.stop(true);
				leftMotor.stop();
				return;
			case ABANDON:
				Sound.buzz();
				return;
			}
			pauseThread();
		}
	}
	
	
	
	private void toggleWheels() {
		rightMotor.setSpeed(SHUFFLE_SPEED);
		leftMotor.setSpeed(SHUFFLE_SPEED);
		if (whitchMotor) {
			rightMotor.stop(true);
			leftMotor.rotate(ROTATION, false);
		} else {
			leftMotor.stop(true);
			rightMotor.rotate(ROTATION, false);
		}
		whitchMotor = !whitchMotor;
	}
	
	/**
	 * Gets the state of the Block searching state machine.
	 * @return  The blockSearchingState of the state machine
	 */
	public BlockSearchingState getBlockSearchingState() {
		synchronized(stateLock) {
			return blockSearchingState;
		}
	}
	
	/**
	 * Sets a new state for the block searching state machine
	 * @param state  The new state to be set
	 */
	public void setBlockSearchingState(BlockSearchingState state) {
		synchronized(stateLock) {
			blockSearchingState = state;
		}
	}
	
	/**
	 * This method allows the thread to pause itself
	 */
	private void pauseThread() {
		if (getBlockSearchingState() == BlockSearchingState.SEARCHING) {
			paused = true;
			synchronized (pauseLock) {
	            if (paused) {
	                try {
	                    pauseLock.wait(300); //TODO figure out the timeout
	                } catch (InterruptedException ex) {
	                }
	            }
	        }
		}
	}
	
	/**
	 * This method wakes up all threads that were paused within this instance of BlockSearchingController instance
	 */
    public void resumeThread() {
    	setBlockSearchingState(BlockSearchingState.FOUND);
        synchronized (pauseLock) {
        	if (paused) {
        		paused = false;
        		pauseLock.notifyAll(); // Unblocks thread
        	}
        }
    }
    
	
	
	/**
	 * Initializes the variables to determine the search area
	 * @param teamNumber  The team number
	 */
	private void determineSearchMethod(int teamNumber) {
		boolean greenTeam = (gameParameters.GreenTeam == teamNumber);
		if(greenTeam) {
			searchMethod = navigationController.mapPoint(gameParameters.SG_LL) != Zone.RIVER && navigationController.mapPoint(gameParameters.SG_UR) != Zone.RIVER;
			lowerLeft = gameParameters.SG_LL;
			upperRight = gameParameters.SG_UR;
		} else {
			searchMethod = navigationController.mapPoint(gameParameters.SR_LL) != Zone.RIVER && navigationController.mapPoint(gameParameters.SR_UR) != Zone.RIVER;
			lowerLeft = gameParameters.SR_LL;
			upperRight = gameParameters.SR_UR;
		}
		
		if(searchMethod && upperRight.x - lowerLeft.x > 2 * tile && upperRight.y - lowerLeft.y > 2 * tile) {
			searchMethod = ! searchMethod;
		}
	}
	
	/**
	 * This method returns the closest corner of the opponent search area from the current robot position
	 * indicated from the odometer.
	 * @return  The coordinate
	 */
	private Coordinate closestValidPoint() {
		int currentX = (int) odometer.getX();
		int currentY = (int) odometer.getY();
		double x = (Math.abs(currentX - lowerLeft.x) > Math.abs(currentX - upperRight.x)) ? upperRight.x : lowerLeft.x;
		double y = (Math.abs(currentY - lowerLeft.y) > Math.abs(currentY - upperRight.y)) ? upperRight.y : lowerLeft.y;
		if (x == lowerLeft.x && y == lowerLeft.x) {
			xIncrement = 1;
			yIncrement = 1;
			return lowerLeft;
		}
		else if (x == upperRight.x && y == upperRight.y) {
			xIncrement = -1;
			yIncrement = -1;
			return upperRight;
		}
		else if (x == lowerLeft.x && y == upperRight.y) {
			xIncrement = 1;
			yIncrement = -1;
		}
		else if (x == upperRight.x && y == lowerLeft.y) {
			xIncrement = -1;
			yIncrement = 1;
		}
		return new Coordinate(x, y);
	}
	
	/**
	 * This method increments the next coordinate where a sweep needs to be performed
	 * and makes sure that the coordinate is valid to be accessed by the robot.
	 */
	private void incrementNextSweep() {
		double newX = nextSweep.x;
		double newY = nextSweep.y + (yIncrement * tile);
		if (navigationController.mapPoint(newX, newY) == Zone.RIVER || !isInSearchArea(newX, newY)) {
			yIncrement = -yIncrement;
			newY = nextSweep.y;
			newX = nextSweep.x + (xIncrement * tile);
		}
		nextSweep = new Coordinate(newX, newY);
	}
	
	/**
	 * Determines if a given coordinate is within a search area
	 * @param node  The node that needs checking
	 * @return  True if the node is in the search area
	 */
	private boolean isInSearchArea(Coordinate node) {
		if (node.x >= lowerLeft.x && node.x <= upperRight.x && node.y >= lowerLeft.y && node.y <= upperRight.y) {
			return true;
		}
		return false;
	}
	
	/**
	 * Determines if a given coordinate is within a search area
	 * @param x  The X coordinate
	 * @return y  The Y coordinate
	 */
	private boolean isInSearchArea(double x, double y) {
		if (x >= lowerLeft.x && x <= upperRight.x && y >= lowerLeft.y && y <= upperRight.y) {
			return true;
		}
		return false;
	}
}
