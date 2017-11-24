/**
 * BlockSearchingData.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * BlockSearchingData processes data coming from the color sensor to determine the color
 * of the block in front of it.
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 * @author Zeyu Chen
 *
 */

public class BlockSearchingData {
	
	private GameParameters gameParameters;
	
	//Target color to find
	private int target;
	private final int teamNumber;
	private int counter = 0;
	private static final int THRESHOLD = 4;
	
	//Associations
	private BlockSearchingController blockSearchingController;
	
	
	/**
	 * Creates an instance of BlocksearchingData
	 * @param gameParameters  Parameters in the current game containing the coordinates of the search area.
	 * @param teamNumber  The team number
	 */
	public BlockSearchingData(GameParameters gameParameters, int teamNumber) {
		this.gameParameters = gameParameters;
		this.teamNumber = teamNumber;
		determineTargetColor();
	}
	
	/**
	 * Sends an interrupt to the blockSearchingController if the block is found.
	 * @param dataFront  ColorID Data from the front color sensor
	 * @param dataSide  ColorID data from the side color sensor
	 */
	public void processData(int dataFront, int dataSide) {
		if(dataFront == target || dataSide == target) {
			counter++;
			if (counter > THRESHOLD) {
				blockSearchingController.resumeThread();
			}
		} else {
			counter = 0;
		}
	}
	
	/**
	 * This method sets the controller to notify to when the block is found
	 * @param blockSearchingController  The controller to set
	 */
	public void setBlockSeachingController(BlockSearchingController blockSearchingController) {
		this.blockSearchingController = blockSearchingController;
	}
	
	private void determineTargetColor() {
		int target;
		if (gameParameters.GreenTeam == teamNumber) {
			target = gameParameters.OG;
		} else {
			target = gameParameters.OR;
		}
		switch (target) {
		case 1: //Red
			target = 0;
			break;
		case 2: //Blue
			target = 2;
			break;
		case 3: //Yellow
			target = 3;
			break;
		case 4: //White
			target = 6;
			break;
		default:
			target = -1;
			break;
		}
	}
}
