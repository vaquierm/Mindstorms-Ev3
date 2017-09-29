// Lab2.java

package ca.mcgill.ecse211.odometerlab;

import java.util.ArrayList;
import java.util.List;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class LocalisationLab {

	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));

	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	private static final Port colorPort = LocalEV3.get().getPort("S1");
	private static final Port usPort = LocalEV3.get().getPort("S2");

	public static Odometer odometer;

	private static final int bandCenter = 40; // Offset from the wall (cm)
	private static final int bandWidth = 3; // Width of dead band (cm)

	public static final double WHEEL_RADIUS = 2.1;
	public static final double TRACK = 17;

	public static void main(String[] args) {
		int buttonChoice;

		final TextLCD t = LocalEV3.get().getTextLCD();
		odometer = new Odometer(leftMotor, rightMotor);
		OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t);

		@SuppressWarnings("resource") // Because we don't bother to close this
										// resource
		SensorModes colorSensor = new EV3ColorSensor(colorPort); // colorSensor
																	// is the
																	// instance
		SampleProvider colorRed = colorSensor.getMode("Red"); // colorRed
																// provides
																// samples from
																// this instance
		float[] colorRedData = new float[colorRed.sampleSize()]; // colorRedData
																	// is the
																	// buffer in
																	// which
																	// data are
																	// returned

		@SuppressWarnings("resource") // Because we don't bother to close this
										// resource
		SensorModes usSensor = new EV3UltrasonicSensor(usPort); // usSensor is
																// the instance
		SampleProvider usDistance = usSensor.getMode("Distance"); // usDistance
																	// provides
																	// samples
																	// from this
																	// instance
		float[] usData = new float[usDistance.sampleSize()]; // usData is the
																// buffer in
																// which data
																// are
																// returned

		OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, colorRed, colorRedData);
		Navigation nav = new Navigation(odometer, leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
		PController pController = new PController(bandCenter, bandWidth);
		UltrasonicPoller usPoller = new UltrasonicPoller(nav, usDistance, usData, pController);

		do {
			// clear the display
			t.clear();

			// ask the user whether the motors should drive in a square or float
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Simple|Obstacle", 0, 2);
			t.drawString("  nav  | avoid  ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

		if (buttonChoice == Button.ID_LEFT) {

			List<Coordinate> coordinateList = new ArrayList<Coordinate>();
			coordinateList.add(new Coordinate(30, 60));
			coordinateList.add(new Coordinate(0, 60));
			coordinateList.add(new Coordinate(60, 60));
			coordinateList.add(new Coordinate(30, 60));
			coordinateList.add(new Coordinate(30, 0));

			new NavigationController(usPoller, odometer, nav, coordinateList, false).start();

			odometryDisplay.start();

		} else {
			// clear the display
			t.clear();

			// ask the user whether the motors should drive in a square or float
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Simple|Obstacle", 0, 2);
			t.drawString("  nav  | avoid  ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();


			if (buttonChoice == Button.ID_RIGHT) {
				odometryCorrection.start();

				List<Coordinate> coordinateList = new ArrayList<Coordinate>();
				coordinateList.add(new Coordinate(0, 60));
				coordinateList.add(new Coordinate(60, 0));
				

				new NavigationController(usPoller, odometer, nav, coordinateList, true).start();
				
				odometryDisplay.start();
			}
		}

		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
