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
	
	//Associations
	private BlockSearchingController blockSearchingController;
	
	private int[] rgbData = new int[3];
	
	/**
	 * Creates an instance of BlocksearchingData
	 * @param gameParameters  Parameters in the current game containing the coordinates of the search area.
	 */
	public BlockSearchingData(GameParameters gameParameters) {
		this.gameParameters = gameParameters;
	}
	
	public void processData(float[] rgb) {
		rgbData[0] = (int) (rgb[0] * 100);
		rgbData[1] = (int) (rgb[1] * 100);
		rgbData[2] = (int) (rgb[2] * 100);
		System.out.println(rgbData[0]+", "+rgbData[1]+", "+rgbData[2]);

		
		//TODO this will take a length 3 array and process the data to determine if its a block
	}
	
	/**
	 * This method sets the controller to notify to when the block is found
	 * @param blockSearchingController  The controller to set
	 */
	public void setBlockSeachingController(BlockSearchingController blockSearchingController) {
		this.blockSearchingController = blockSearchingController;
	}
}
