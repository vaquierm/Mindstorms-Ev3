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

	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	public static final EV3LargeRegulatedMotor armMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	public static final EV3MediumRegulatedMotor USMotor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("D"));

	private static final Port colorHorizontalPort = LocalEV3.get().getPort("S1");
	private static final Port colorVerticalPort = LocalEV3.get().getPort("S2");
	private static final Port usPort = LocalEV3.get().getPort("S3");
	private static final Port gyroPort = LocalEV3.get().getPort("S4");

	public static final Odometer odometer;

	//Subtask controllers
	public static final NavigationController navigationController;
	public static final Navigation navigation;
	public static final LocalisationController localisationController;
	public static final Localisation localisation;
	public static final ZiplineCrossingController ziplineCrossingController;
	public static final FlagSearchingController flagSearchingController;

	//Environmental and robot constants
	public static final double TILE = 30.48;
	public static final double WHEEL_RADIUS = 2.1;
	public static final double TRACK = 17;

	public static void main(String[] args) {

		final TextLCD t = LocalEV3.get().getTextLCD();
		odometer = new Odometer(leftMotor, rightMotor);
		Display odometryDisplay = new Display(odometer, t);

		//Instantiate sensors
		@SuppressWarnings("resource") 
		SensorModes colorSensorHorizontal = new EV3ColorSensor(colorHorizontalPort);
		SampleProvider colorHorizontalRed = colorSensorHorizontal.getMode("Red");
		SampleProvider colorHorizontalRedMean = new MeanFilter(colorHorizontalRed, 7);
		float[] colorHorizontalRedData = new float[colorHorizontalRedMean.sampleSize()];
		
		SensorModes colorSensorVertical = new EV3ColorSensor(colorVerticalPort);
		SampleProvider colorVerticalRed = colorSensorVertical.getMode("Red");
		SampleProvider colorVerticalRedMean = new MeanFilter(colorVerticalRed, 7);
		float[] colorVerticalRedData = new float[colorVerticalRedMean.sampleSize()];

		SensorModes usSensor = new EV3UltrasonicSensor(usPort);
		SampleProvider usDistance = usSensor.getMode("Distance");
		SampleProvider meanFilterUs = new MeanFilter(usDistance, 7);
		float[] usData = new float[meanFilterUs.sampleSize()];
		
		SensorModes gyroSensor = new EV3GyroSensor(gyroPort);
		SampleProvider gyroAngle = gyroSensor.getMode("angle");
		SampleProvider meanFilterGyro = new MeanFilter(gyroAngle, 7);
		float[] gyroData = new float[meanFilterGyro.sampleSize()];


		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
