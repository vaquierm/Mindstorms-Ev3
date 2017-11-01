/**
 * LocalisationController.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * The localisation manager holds the control flow to the localisation procedure
 * It holds a reference to an instance of a localisation class which hold the specific logic for subtasks of the localisation procedure
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class LocalisationController {
	
	//Associations
	private Localisation localisation;
	private Navigation navigation;
	
	//Game board constants
	private final double tile;
	private final int startingCorner;
	private final int boardSize;
	
	/**
	 * Creates a LocalisationController object.
	 * @param localisation
	 * @param navigation
	 * @param tile
	 * @param startingCorner
	 * @param boardSize
	 */
	public LocalisationController(Localisation localisation, Navigation navigation, double tile, int startingCorner, int boardSize) {
		this.localisation = localisation;
		this.navigation = navigation;
		this.tile = tile;
		this.startingCorner = startingCorner;
		this.boardSize = boardSize;
	}

	/**
	 * Performs the initial localisation routine involving
	 * an ultrasonic localisation, navigation to the closest intersection, then color localisation.
	 */
	private void initialLocalisationRoutine() {
		localisation.usLocalisation();
		navigateToInitialIntersection();
		localisation.colorLocalisation();
	}
	
	/**
	 * Makes the robot navigate to the closest intersection point from its starting position.
	 */
	private void navigateToInitialIntersection() {
		switch(startingCorner) {
		case 0:
			navigation.travelTo(tile, tile, false);
			break;
		case 1:
			navigation.travelTo(((boardSize - 1) * tile), tile, false);
			break;
		case 2:
			navigation.travelTo(((boardSize - 1) * tile), ((boardSize - 1) * tile), false);
			break;
		case 3:
			navigation.travelTo(tile, ((boardSize - 1) * tile), false);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Performs a localisation routine with the assumption that the robot's center of rotation in on or close to the intersection
	 * of two line
	 */
	private void colorLocalisationRoutine() {
		localisation.colorLocalisation();
	}
	
}

