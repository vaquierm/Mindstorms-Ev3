/**
 * WifiGameParameters.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.Map;
import ca.mcgill.ecse211.WiFiClient.WifiConnection;

/**
 * WifiGameParameters class is meant to connect to a server and wait for game parameters
 * Once the parameters are received, it creates a GameParameters object
 * 
 * @author Michael Vaquier
 *
 */

public class WiFiGameParameters {

	// ** Set these as appropriate for your team and current situation **
	private static final String SERVER_IP = "192.168.2.34";
	private static final int TEAM_NUMBER = 20;

	// Enable/disable printing of debug info from the WiFi class
	private static final boolean ENABLE_DEBUG_WIFI_PRINT = false;

	/**
	 * Returns the game parameters sent over WiFi
	 * @param TILE  Length of the length of the side of a tile (cm)
	 * @return  Game parameters received from the WiFi connection
	 */
	public static GameParameters getGameParameters(double TILE) {
		// Initialize WifiConnection class
		WifiConnection conn = new WifiConnection(SERVER_IP, TEAM_NUMBER, ENABLE_DEBUG_WIFI_PRINT);
		
		try {
		      /*
		       * getData() will connect to the server and wait until the user/TA presses the "Start" button
		       * in the GUI on their laptop with the data filled in. Once it's waiting, you can kill it by
		       * pressing the upper left hand corner button (back/escape) on the EV3. getData() will throw
		       * exceptions if it can't connect to the server (e.g. wrong IP address, server not running on
		       * laptop, not connected to WiFi router, etc.). It will also throw an exception if it connects
		       * but receives corrupted data or a message from the server saying something went wrong. For
		       * example, if TEAM_NUMBER is set to 1 above but the server expects teams 17 and 5, this robot
		       * will receive a message saying an invalid team number was specified and getData() will throw
		       * an exception letting you know.
		       */
		      Map data = conn.getData();
		      
		      return new GameParameters(((Long)data.get("RedTeam")).intValue(), ((Long)data.get("GreenTeam")).intValue(),
		    		  ((Long)data.get("RedCorner")).intValue(), ((Long)data.get("GreenCorner")).intValue(),
		    		  ((Long)data.get("OG")).intValue(), ((Long)data.get("OR")).intValue(),
		    		  new Coordinate(((Long)data.get("Red_LL_x")).intValue() * TILE, ((Long)data.get("Red_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("Red_UR_x")).intValue() * TILE, ((Long)data.get("Red_UR_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("Green_LL_x")).intValue() * TILE, ((Long)data.get("Green_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("Green_UR_x")).intValue() * TILE, ((Long)data.get("Green_UR_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("ZC_R_x")).intValue() * TILE, ((Long)data.get("ZC_R_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("ZO_R_x")).intValue() * TILE, ((Long)data.get("ZO_R_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("ZC_G_x")).intValue() * TILE, ((Long)data.get("ZC_G_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("ZO_G_x")).intValue() * TILE, ((Long)data.get("ZO_G_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SH_LL_x")).intValue() * TILE, ((Long)data.get("SH_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SH_UR_x")).intValue() * TILE, ((Long)data.get("SH_UR_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SV_LL_x")).intValue() * TILE, ((Long)data.get("SV_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SV_UR_x")).intValue() * TILE, ((Long)data.get("SV_UR_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SR_LL_x")).intValue() * TILE, ((Long)data.get("SR_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SR_UR_x")).intValue() * TILE, ((Long)data.get("SR_UR_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SG_LL_x")).intValue() * TILE, ((Long)data.get("SG_LL_y")).intValue() * TILE),
		    		  new Coordinate(((Long)data.get("SG_UR_x")).intValue() * TILE, ((Long)data.get("SG_UR_y")).intValue() * TILE)
		    		  );
		      
		} catch (Exception e) {
		      System.err.println("Error: " + e.getMessage());
	    }
		return null;
	}

}
