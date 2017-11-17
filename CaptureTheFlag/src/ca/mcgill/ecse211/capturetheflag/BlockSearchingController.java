/**
 * BlockSearchingController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.Arrays;

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
	
	private boolean firstX = true;
	private boolean firstY = true;
	private int xIncrement;
	private int yIncrement;
	private int xWidth = 0;
	private int yWidth = 0;
	private int xCounter = 0;
	private int yCounter = 0;
	private boolean travelDir;
	private boolean firstTravel = true;
	
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
	
	/**
	 * this enumeration delimitates the states in which the block searching controller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum BlockSearchingState {SEARCHING, FOUND, ABANDON}
	
	/**
	 * Constructs a BlockSearchingController instance
	 * @param odometer  The odometer association
	 * @param navigationController  The NavigationController association
	 * @param navigation  The Navigation association
	 * @param localisation  The localisation association
	 * @param rightMotor  The reference to the right motor
	 * @param leftMotor  The reference to the left motor
	 * @param colorPoller  The association to the CollorPoller
	 * @param gameParameters  The game parameters of the round
	 * @param tile  The width of a tile in centimeters
	 * @param teamNumber  The team number of the robot
	 */
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
		
		inintialisation(teamNumber);
		
		colorPoller.getBlockSearchingData().setBlockSeachingController(this);
	}
	
	/**
	 * This method runs the subtask involving the block searching component of the
	 * competition.
	 */
	public void runBlockSearchingTask() {
		navigationController.addWayPoint(nextSweep);
		navigationController.runNavigationTask(true);
		setBlockSearchingState(BlockSearchingState.SEARCHING);
		colorPoller.startPolling(ColorPollingState.BLOCK_SEARCHING);
		spinMethod();
		colorPoller.stopPolling();
	}
	
	/**
	 * Search method where the robot navigates to every intersection of the search area and performs a localisation sweep combination.
	 */
	private void spinMethod() {
		boolean localise = true;
		while (true) {
			switch (getBlockSearchingState()) {
			case SEARCHING:
				if (!rightMotor.isMoving() && !leftMotor.isMoving()) {
					if (localise) {
						localisation.colorLocalisation(true);
					}
					localise = !localise;
					if (getBlockSearchingState() == BlockSearchingState.SEARCHING) {
						incrementNextSweep();
						if (isInSearchArea(nextSweep)) {
							navigation.travelTo(nextSweep.x, nextSweep.y, true);
						} else {
							localisation.colorLocalisation(true);
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
	private void inintialisation(int teamNumber) {
		boolean greenTeam = (gameParameters.GreenTeam == teamNumber);
		if(greenTeam) {
			lowerLeft = gameParameters.SG_LL;
			upperRight = gameParameters.SG_UR;
		} else {
			lowerLeft = gameParameters.SR_LL;
			upperRight = gameParameters.SR_UR;
		}
		closestValidPoint(greenTeam);
	}
	
	/**
	 * This method returns the closest corner of the opponent search area from the current robot position
	 * indicated from the odometer.
	 * @return  The coordinate
	 */
	private void closestValidPoint(boolean greenTeam) {
		double startX, startY;
		if (greenTeam) {
			startX = gameParameters.ZO_R.x;
			startY = gameParameters.ZO_R.y;
		} else {
			startX = (gameParameters.SV_UR.x + gameParameters.SV_LL.x) / 2;
			startY = (gameParameters.SH_UR.y + gameParameters.SH_LL.y) / 2;
		}
		double LL, LR, UL, UR;
		LL = Math.abs(lowerLeft.x - startX) + Math.abs(lowerLeft.y - startY);
		UR = Math.abs(upperRight.x - startX) + Math.abs(upperRight.y - startY);
		LR = Math.abs(upperRight.x - startX) + Math.abs(lowerLeft.y - startY);
		UL = Math.abs(lowerLeft.x - startX) + Math.abs(upperRight.y - startY);
		
		double[] distances = {LL, LR, UL, UR};
		
		Arrays.sort(distances);
		
		if(tryCorner(distances[0], LL, LR, UL, UR))
			return;
		
		if(tryCorner(distances[2], LL, LR, UL, UR))
			return;
		
		if(tryCorner(distances[3], LL, LR, UL, UR))
			return;
		
		if(tryCorner(distances[4], LL, LR, UL, UR))
			return;
		
		nextSweep = new Coordinate((upperRight.x - lowerLeft.x), (upperRight.y - lowerLeft.y));
	}
	
	/**
	 * Tries to initialize nextSweep as a corner of the search area
	 * @param target  The distance target
	 * @param LL  Distance form lower left corner
	 * @param LR  Distance form lower right corner
	 * @param UL  Distance form upper left corner
	 * @param UR  Distance from upper right corner
	 * @return  If the initialization was successful
	 */
	private boolean tryCorner(double target, double LL, double LR, double UL, double UR) {
		if (target == LL) {
			xIncrement = 1;
			yIncrement = 1;
			travelDir = true;
			nextSweep = lowerLeft;
			if (navigationController.mapPoint(nextSweep) == Zone.RIVER) {
				return tryAdjacentPoints();
			}
			return true;
		} else if (target == LR) {
			xIncrement = -1;
			yIncrement = 1;
			travelDir = false;
			nextSweep = new Coordinate(upperRight.x, lowerLeft.y);
			if (navigationController.mapPoint(nextSweep) == Zone.RIVER) {
				return tryAdjacentPoints();
			}
			return true;
		} else if (target == UL) {
			xIncrement = 1;
			yIncrement = -1;
			travelDir = false;
			nextSweep = new Coordinate(upperRight.x, lowerLeft.y);
			if (navigationController.mapPoint(nextSweep) == Zone.RIVER) {
				return tryAdjacentPoints();
			}
			return true;
		} else {
			xIncrement = -1;
			yIncrement = -1;
			travelDir = true;
			nextSweep = upperRight;
			if (navigationController.mapPoint(nextSweep) == Zone.RIVER) {
				return tryAdjacentPoints();
			}
			return true;
		}
	}
	
	/**
	 * Tries to initialize the nextSweep as one of the adjacent point from the current corner attempt
	 * @return  If the initialization was successful
	 */
	private boolean tryAdjacentPoints() {
		if (navigationController.mapPoint(nextSweep.x, nextSweep.y + (yIncrement * tile)) != Zone.RIVER && !isCorner(nextSweep.x, nextSweep.y + (yIncrement * tile))) {
			nextSweep = new Coordinate(nextSweep.x, nextSweep.y + (yIncrement * tile));
			return true;
		} else if (navigationController.mapPoint(nextSweep.x + (xIncrement * tile), nextSweep.y) != Zone.RIVER && !isCorner(nextSweep.x + (xIncrement * tile), nextSweep.y)) {
			nextSweep = new Coordinate(nextSweep.x + (xIncrement * tile), nextSweep.y);
			return true;
		}
		return false;
	}
	
	/**
	 * The input coordinates are one of the corners of the search area
	 * @param x  The x coordinate to check
	 * @param y  The y coordinate to check
	 * @return  True if the input is one of the corners.
	 */
	private boolean isCorner(double x, double y) {
		if ((x == lowerLeft.x && y == lowerLeft.y) || (x == lowerLeft.x && y == upperRight.y) || (x == upperRight.x && y == lowerLeft.y) || (x == upperRight.x && y == upperRight.y))
			return true;
		return false;
	}
	
	/**
	 * This method increments the next coordinate where a sweep needs to be performed
	 * and makes sure that the coordinate is valid to be accessed by the robot.
	 */
	private void incrementNextSweep() {
		if (!firstTravel && ((xWidth <= 0 && yWidth <=0) || (!travelDir && xWidth == 0) || (travelDir && yWidth == 0))) {
			nextSweep = new Coordinate(-1, -1);
			return;
		}
		double newX, newY;
		if (travelDir) {
			newX = nextSweep.x;
			newY = nextSweep.y + (yIncrement * tile);
			if (firstY) {
				if (isInSearchArea(newX, newY)) {
					yWidth++;
				} else {
					newX = nextSweep.x + (xIncrement * tile);
					newY = nextSweep.y;
					if (!isInSearchArea(newX, newY)) {
						nextSweep = new Coordinate(-1, -1);
						return;
					}
					yIncrement = -yIncrement;
					firstY = false;
					travelDir = false;
					if (firstTravel) {
						xWidth++;
						firstTravel = !firstTravel;
					} else {
						xCounter++;
						yWidth--;
					}
				}
			} else {
				yCounter++;
				if (yCounter >= yWidth) {
					yIncrement = -yIncrement;
					travelDir = false;
					yWidth--;
					yCounter = 0;
					if (xWidth < 0) {
						nextSweep = new Coordinate(-1, -1);
						return;
					}
				}
			}
		} else {
			newX = nextSweep.x + (xIncrement * tile);
			newY = nextSweep.y;
			if (firstX) {
				if (isInSearchArea(newX, newY)) {
					xWidth++;
				} else {
					newX = nextSweep.x;
					newY = nextSweep.y + (yIncrement * tile);
					if (!isInSearchArea(newX, newY)) {
						nextSweep = new Coordinate(-1, -1);
						return;
					}
					xIncrement = -xIncrement;
					firstX = false;
					travelDir = true;
					if (firstTravel) {
						yWidth++;
						firstTravel = !firstTravel;
					} else {
						yCounter++;
						xWidth--;
					}
				}
			} else {
				xCounter++;
				if (xCounter >= xWidth) {
					xIncrement = -xIncrement;
					travelDir = true;
					xWidth--;
					xCounter = 0;
					if (yWidth < 0) {
						nextSweep = new Coordinate(-1, -1);
						return;
					}
				}
			}
		}
		
		nextSweep = new Coordinate(newX, newY);
	}
	
	/**
	 * Determines if a given coordinate is within a search area
	 * @param node  The node that needs checking
	 * @return  True if the node is in the search area
	 */
	private boolean isInSearchArea(Coordinate node) {
		if (node.x >= lowerLeft.x && node.x <= upperRight.x && node.y >= lowerLeft.y && node.y <= upperRight.y && navigationController.mapPoint(node) != Zone.RIVER) {
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
		if (x >= lowerLeft.x && x <= upperRight.x && y >= lowerLeft.y && y <= upperRight.y && navigationController.mapPoint(x, y) != Zone.RIVER) {
			return true;
		}
		return false;
	}
}
