/**
 * MainController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.LinkedList;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
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

	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor armMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3MediumRegulatedMotor frontMotor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("D"));

	private static final Port colorHorizontalPort = LocalEV3.get().getPort("S1");
	private static final Port colorVerticalPort = LocalEV3.get().getPort("S2");
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
//	private static final FlagSearchingController flagSearchingController;
	
	//Pollers
	private static UltrasonicPoller ultrasonicPoller;
	private static ColorPoller colorPoller;
	
	//Data Processing
	private static ColorLocalisationData colorLocalisation;
	private static ZiplineLightData ziplineLightData;
	private static UltrasonicLocalisationData ultrasonicLocalisationData;
	private static UltrasonicNavigationData ultrasonicNavigationData;

	//Environmental and robot constants
	private static final double TILE = 30.48;
	private static final int BOARD_SIZE = 12;
	private static final double WHEEL_RADIUS = 2.1;
	private static final double TRACK = 17;

	public static void main(String[] args) {		

		//Instantiate sensors
		@SuppressWarnings("resource") 
		SensorModes colorSensorHorizontal = new EV3ColorSensor(colorHorizontalPort);
		SampleProvider colorHorizontalRed = colorSensorHorizontal.getMode("Red");
		SampleProvider colorHorizontalRedMean = new MeanFilter(colorHorizontalRed, 7);
		float[] colorHorizontalRedData = new float[colorHorizontalRedMean.sampleSize()];
		
		@SuppressWarnings("resource") 
		SensorModes colorSensorVertical = new EV3ColorSensor(colorVerticalPort);
		SampleProvider colorVerticalRed = colorSensorVertical.getMode("Red");
		SampleProvider colorVerticalRedMean = new MeanFilter(colorVerticalRed, 7);
		float[] colorVerticalRedData = new float[colorVerticalRedMean.sampleSize()];
		
		@SuppressWarnings("resource") 
		SensorModes colorSensorSide = new EV3ColorSensor(colorSidePort);
		SampleProvider colorSideRed = colorSensorSide.getMode("Red");
		SampleProvider colorSideRedMean = new MeanFilter(colorSideRed, 7);
		float[] colorSideRedData = new float[colorSideRedMean.sampleSize()];

		@SuppressWarnings("resource") 
		SensorModes usSensor = new EV3UltrasonicSensor(usPort);
		SampleProvider usDistance = usSensor.getMode("Distance");
		SampleProvider meanFilterUs = new MeanFilter(usDistance, 7);
		float[] usData = new float[meanFilterUs.sampleSize()];
		
		//Get the game parameters
		gameParameters = WiFiGameParameters.getGameParameters(TILE);
		
		final TextLCD t = LocalEV3.get().getTextLCD();
		odometer = new Odometer(leftMotor, rightMotor, WHEEL_RADIUS, TRACK);
		Display odometryDisplay = new Display(odometer, t);
		
		colorLocalisation = new ColorLocalisationData();
		ziplineLightData = new ZiplineLightData();
		ultrasonicLocalisationData = new UltrasonicLocalisationData();
		ultrasonicNavigationData = new UltrasonicNavigationData(rightMotor, leftMotor, TILE, BOARD_SIZE);
		
		colorPoller = new ColorPoller(colorLocalisation, colorVerticalRedMean, colorVerticalRedData, colorHorizontalRedMean, colorHorizontalRedData, colorSideRedMean, colorSideRedData);
		ultrasonicPoller = new UltrasonicPoller(meanFilterUs, usData, ultrasonicLocalisationData, ultrasonicNavigationData);
		
		navigation = new Navigation(odometer, rightMotor, leftMotor, WHEEL_RADIUS, TRACK);
		localisation = new Localisation(odometer, navigation, ultrasonicPoller, colorPoller, rightMotor, leftMotor, TILE, 0); //TODO startingCorner
		
		navigationController = new NavigationController(rightMotor, leftMotor, frontMotor, odometer, navigation, ultrasonicPoller, gameParameters);
		localisationController = new LocalisationController(localisation, navigation, TILE, 0, BOARD_SIZE); //TODO startingCorner
		ziplineController = new ZiplineController(odometer, colorPoller, rightMotor, leftMotor, armMotor, gameParameters);
		


		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
