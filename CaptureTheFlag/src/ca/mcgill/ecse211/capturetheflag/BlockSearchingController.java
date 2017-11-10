/**
 * BlockSearchingController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import ca.mcgill.ecse211.capturetheflag.ColorPoller.ColorPollingState;

/**
 * BlockSearchingController is the subtask involving finding a block of a specific
 * Color in a specified search area
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 * @author Zeyu Chen
 */

public class BlockSearchingController {
	
	private GameParameters gameParamaters;
	
	//Associations
	private ColorPoller colorPoller;
	
	
	public BlockSearchingController(ColorPoller colorPoller, GameParameters gameParameters) {
		this.colorPoller = colorPoller;
		this.gameParamaters = gameParameters;
		
		colorPoller.getBlockSearchingData().setBlockSeachingController(this);
	}
	
	/**
	 * This method runs the subtask involving the block searching component of the
	 * competition.
	 */
	public void runBlockSearchingTask() {
		//TODO this is the method that contains the logic for block searching
		colorPoller.startPolling(ColorPollingState.BLOCK_SEARCHING);
	}

}
