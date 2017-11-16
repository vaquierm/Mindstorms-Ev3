/**
 * BlockSearchingData.java
 */

package ca.mcgill.ecse211.capturetheflag;

import ca.mcgill.ecse211.capturetheflag.BlockSearchingController.BlockSearchingState;

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
	
	public void processData(int data) {
		if(data == target) {
			blockSearchingController.resumeThread();
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
		case 1:
			target = 0;
			break;
		case 2:
			target = 2;
			break;
		case 3:
			target = 3;
			break;
		case 4:
			target = 6;
			break;
		default:
			target = -1;
			break;
		}
	}
}
