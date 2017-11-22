/**
 * MainController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.io.File;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;

/**
 * 
 * Main Controller contains the main flow of the game and calls sub tasks in a
 * sequential manner to complete the game. Sensor and motors are set up in this
 * class to be used in the subtasks.
 * 
 * @author Michael Vaquier
 * 
 */
public class MainController {
	
	private static final int TEAM_NUMBER = 20;
	private static boolean greenTeam;

	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3LargeRegulatedMotor armMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3MediumRegulatedMotor frontMotor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));

	private static final Port colorHorizontalPort = LocalEV3.get().getPort("S2");
	private static final Port colorVerticalPort = LocalEV3.get().getPort("S1");
	private static final Port colorSidePort = LocalEV3.get().getPort("S3");
	private static final Port usPort = LocalEV3.get().getPort("S4");
	
	private static GameParameters gameParameters;

	private static Odometer odometer;
	private static Localisation localisation;
	private static Navigation navigation; 

	//Subtask controllers
	private static NavigationController navigationController;
	private static LocalisationController localisationController;
	private static ZiplineController ziplineController;
	private static BlockSearchingController blockSearchingController;
	
	//Pollers
	private static UltrasonicPoller ultrasonicPoller;
	private static ColorPoller colorPoller;
	
	//Data Processing
	private static ColorLocalisationData colorLocalisationData;
	private static ZiplineLightData ziplineLightData;
	private static UltrasonicLocalisationData ultrasonicLocalisationData;
	private static UltrasonicNavigationData ultrasonicNavigationData;
	private static BlockSearchingData blockSearchingData;

	//Environmental and robot constants
	private static int startingCorner;
	private static final double TILE = 30.48;
	private static final int BOARD_SIZE = 8; //TODO change for competition
	private static final double WHEEL_RADIUS = 2.1;
	private static final double TRACK = 9.71; //9.92
	
	
	//This code can be used to find the timing of threads.
	/*
	private static Object lock = new Object();
	
	private static int T1 = 0;
	private static int T2 = 0;
	
	public static void toggleT1() {
		synchronized(lock) {
			long time = System.currentTimeMillis();
			System.out.println(time + ", " + T1 +", "+ T2 );
			T1 = (T1 == 0) ? 1 : 0;
			System.out.println(time+1 + ", " + T1 +", "+T2 );
		}
	}
	
	public static void toggleT2() {
		synchronized(lock) {
			long time = System.currentTimeMillis();
			System.out.println(time + ", " + T1 +", "+ T2 );
			T2 = (T2 == 0) ? 1 : 0;
			System.out.println(time+1 + ", " + T1 +", "+ T2 );
		}
	}*/

	public static void main(String[] args) {		

		//Instantiate sensors
		@SuppressWarnings("resource") 
		SensorModes colorSensorHorizontal = new EV3ColorSensor(colorHorizontalPort);
		SampleProvider colorHorizontalRed = colorSensorHorizontal.getMode("ColorID");
		SampleProvider colorHorizontalRedMean = new MeanFilter(colorHorizontalRed, 7);
		float[] colorHorizontalRedData = new float[colorHorizontalRedMean.sampleSize()];
		
		@SuppressWarnings("resource") 
		SensorModes colorSensorVertical = new EV3ColorSensor(colorVerticalPort);
		SampleProvider colorVerticalRed = colorSensorVertical.getMode("Red");
		SampleProvider colorVerticalRedMean = new MeanFilter(colorVerticalRed, 7);
		float[] colorVerticalRedData = new float[colorVerticalRedMean.sampleSize()];
		
		@SuppressWarnings("resource") 
		SensorModes colorSensorSide = new EV3ColorSensor(colorSidePort);
		SampleProvider colorSideRed = colorSensorSide.getMode("ColorID");
		SampleProvider colorSideRedMean = new MeanFilter(colorSideRed, 7);
		float[] colorSideRedData = new float[colorSideRedMean.sampleSize()];

		@SuppressWarnings("resource") 
		SensorModes usSensor = new EV3UltrasonicSensor(usPort);
		SampleProvider usDistance = usSensor.getMode("Distance");
		SampleProvider meanFilterUs = new MeanFilter(usDistance, 7);
		float[] usData = new float[meanFilterUs.sampleSize()];
		
		final TextLCD t = LocalEV3.get().getTextLCD();
		t.drawString("  READY  ", 0, 0);
		//Get the game parameters
		

		gameParameters = WiFiGameParameters.getGameParameters(TILE);
		/**
		 * This is the hardcoded game parameters to not have to input them every time.
		 */
		/*while (Button.waitForAnyPress() != Button.ID_ENTER); //TODO delete for beta
		gameParameters = new GameParameters(1 , 20, //Team numbers
	    		  1, 1, //Starting corners
	    		  1, 0, //Color of flags
	    		  new Coordinate(0 * TILE, 5 * TILE), //Red_LL
	    		  new Coordinate(5 * TILE, 8 * TILE), //Red_UR
	    		  new Coordinate(3 * TILE, 0 * TILE), //Green_LL
	    		  new Coordinate(8 * TILE, 3 * TILE), //Green_UR
	    		  new Coordinate(2 * TILE, 6 * TILE), //ZC_R
	    		  new Coordinate(1 * TILE, 7 * TILE), //ZO_R
	    		  new Coordinate(5 * TILE, 3 * TILE), //ZC_G
	    		  new Coordinate(6 * TILE, 2 * TILE), //ZO_G
	    		  new Coordinate(5 * TILE, 6 * TILE), //SH_LL
	    		  new Coordinate(7 * TILE, 7 * TILE), //SH_UR
	    		  new Coordinate(6 * TILE, 3 * TILE), //SV_LL
	    		  new Coordinate(7 * TILE, 7 * TILE), //SV_UR
	    		  new Coordinate(3 * TILE, 7 * TILE), //SR_LL 
	    		  new Coordinate(5 * TILE, 8 * TILE), //SR_UR
	    		  new Coordinate(3 * TILE, 0 * TILE), //SG_LL
	    		  new Coordinate(5 * TILE, 1 * TILE) //SG_UR
	    		  );*/
		
		determineStartingCorner();
		
		odometer = new Odometer(leftMotor, rightMotor, WHEEL_RADIUS, TRACK);
		setEstimateInitialPosition();
		Display odometryDisplay = new Display(odometer, t);
		odometryDisplay.start();
		odometer.startOdometerTimer();
		
		colorLocalisationData = new ColorLocalisationData();
		ziplineLightData = new ZiplineLightData();
		ultrasonicLocalisationData = new UltrasonicLocalisationData();
		ultrasonicNavigationData = new UltrasonicNavigationData(rightMotor, leftMotor, TILE, BOARD_SIZE);
		blockSearchingData = new BlockSearchingData(gameParameters, TEAM_NUMBER);
		
		colorPoller = new ColorPoller(colorVerticalRedMean, colorVerticalRedData, colorHorizontalRedMean, colorHorizontalRedData, colorSideRedMean, colorSideRedData, colorLocalisationData, ziplineLightData, blockSearchingData);
		ultrasonicPoller = new UltrasonicPoller(/*meanFilterUs*/ usDistance, usData, ultrasonicLocalisationData, ultrasonicNavigationData);
		
		navigation = new Navigation(odometer, rightMotor, leftMotor, WHEEL_RADIUS, TRACK, gameParameters);
		localisation = new Localisation(odometer, navigation, ultrasonicPoller, colorPoller, rightMotor, leftMotor, TILE, startingCorner);
		
		navigationController = new NavigationController(rightMotor, leftMotor, frontMotor, odometer, navigation, localisation, ultrasonicPoller, gameParameters, TILE);
		localisationController = new LocalisationController(localisation, navigation, TILE, startingCorner, BOARD_SIZE);
		ziplineController = new ZiplineController(odometer, colorPoller, rightMotor, leftMotor, armMotor, gameParameters);
		blockSearchingController = new BlockSearchingController(odometer, navigationController, navigation, localisation, rightMotor, leftMotor, colorPoller, gameParameters, TILE, TEAM_NUMBER);
		
		
		/*wheelbaseTestRoutine();
		while(Button.waitForAnyEvent() != Button .ID_ENTER);
		navigation.forward(TILE, false);
		while(Button.waitForAnyEvent() != Button .ID_ENTER);*/
		/*
		 * Here is the flow of tasks to run.
		 */
		localisationController.initialLocalisationRoutine();
		
		if(greenTeam) {
			//Travel to zipline
			navigationController.addWayPoint(gameParameters.ZO_G.x, gameParameters.ZO_G.y);
			navigationController.runNavigationTask(true);
			localisationController.colorLocalisationRoutine(true);
			
			//Zipline
			navigation.travelTo(gameParameters.ZO_G.x, gameParameters.ZO_G.y, false);
			navigation.faceZipline();
			ziplineController.runZiplineTask();
			navigation.travelTo(gameParameters.ZO_R.x, gameParameters.ZO_R.y, false);
			localisationController.colorLocalisationRoutine(true);
			
			//BlockSearch
			blockSearchingController.runBlockSearchingTask();
			
			//Travel back
			returnToStartingCorner();
		} else {
			//BlockSearch
			blockSearchingController.runBlockSearchingTask();
			
			//Travel to zipline
			navigationController.addWayPoint(gameParameters.ZO_G.x, gameParameters.ZO_G.y);
			navigationController.runNavigationTask(true);
			localisationController.colorLocalisationRoutine(true);
			
			//Zipline
			navigation.travelTo(gameParameters.ZO_G.x, gameParameters.ZO_G.y, false);
			navigation.faceZipline();
			ziplineController.runZiplineTask();
			navigation.travelTo(gameParameters.ZO_R.x, gameParameters.ZO_R.y, false);
			localisationController.colorLocalisationRoutine(true);
			
			//Travel back
			returnToStartingCorner();
		}
		
		
		while (Button.waitForAnyPress() != Button.ID_ESCAPE);
		System.exit(0);
	}
	
	/**
	 * Makes the robot navigate back to its starting corner
	 */
	private static void returnToStartingCorner() {
		switch (startingCorner) {
		case 0:
			navigationController.addWayPoint(TILE, TILE);
			break;
		case 1:
			navigationController.addWayPoint(((BOARD_SIZE - 1) * TILE), TILE);
			break;
		case 2:
			navigationController.addWayPoint(((BOARD_SIZE - 1) * TILE), ((BOARD_SIZE - 1) * TILE));
			break;
		case 3:
			navigationController.addWayPoint(TILE, ((BOARD_SIZE - 1) * TILE));
			break;
		default:
			break;
		}
		navigationController.runNavigationTask(true);
	}
	
	/**
	 * Gets the game parameters to determine which is the starting corner of the system based
	 * on the team number
	 */
	private static void determineStartingCorner() {
		if (gameParameters.GreenTeam == TEAM_NUMBER) {
			startingCorner = gameParameters.GreenCorner;
			greenTeam = true;
		}
		else if (gameParameters.RedTeam == TEAM_NUMBER) {
			startingCorner = gameParameters.RedCorner;
			greenTeam = false;			
		}
		else
			startingCorner = 0;
	}
	
	/**
	 * Sets the estimate initial position of the robot in the odometer depending on the
	 * game parameters.
	 */
	private static void setEstimateInitialPosition() {
		switch (startingCorner) {
		case 0:
			odometer.setX(20);
			odometer.setY(20);
			break;
		case 1:
			odometer.setX((BOARD_SIZE * TILE) - 20);
			odometer.setY(20);
			break;
		case 2:
			odometer.setX((BOARD_SIZE * TILE) - 20);
			odometer.setY((BOARD_SIZE * TILE) - 20);
			break;
		case 3:
			odometer.setX(20);
			odometer.setY((BOARD_SIZE * TILE) - 20);
			break;
		default:
			break;
		}
	}
	
	/**
	 * This method is used as a test to determine if the wheelbase of the robot is set to the right value
	 */
	private static void wheelbaseTestRoutine() {
		navigation.turnTo(90);
		navigation.turnTo(180);
		navigation.turnTo(270);
		navigation.turnTo(0);
	}
}
