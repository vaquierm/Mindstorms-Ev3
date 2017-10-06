// Lab2.java

package ca.mcgill.ecse211.localisation;

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
import lejos.robotics.filter.MeanFilter;

/**
 * 
 * LocalisationLab is where the robot's display is set up and where
 * the sensors and motors are set up
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class LocalisationLab {

	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));

	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));

	private static final Port colorPort = LocalEV3.get().getPort("S2");
	private static final Port usPort = LocalEV3.get().getPort("S1");

	public static Odometer odometer;


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
		SampleProvider colorRedMean = new MeanFilter(colorRed, 7);
		float[] colorRedData = new float[colorRedMean.sampleSize()]; // colorRedData
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
		SampleProvider meanFilterUs = new MeanFilter(usDistance, 7);
		float[] usData = new float[meanFilterUs.sampleSize()]; // usData is the
																// buffer in
																// which data
																// are
																// returned

		ColorPoller colorPoller = new ColorPoller(odometer, colorRedMean, colorRedData);
		Navigation nav = new Navigation(odometer, leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
		UltrasonicPoller usPoller = new UltrasonicPoller(meanFilterUs, usData);

		
		/*leftMotor.setAcceleration(250);
		rightMotor.setAcceleration(250);
		leftMotor.setSpeed(200);
		rightMotor.setSpeed(200);
		leftMotor.rotate(nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);
		rightMotor.rotate(-nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);*/
		do {
			// clear the display
			t.clear();

			// ask the user whether the motors should drive in a square or float
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Rising|Falling ", 0, 2);
			t.drawString("  edge | edge   ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

		if (buttonChoice == Button.ID_LEFT) {
			odometer.start();

			new LocalisationManager(usPoller, colorPoller, nav, false).start();
			

			odometryDisplay.start();

		} else {
			// clear the display
			t.clear();

			// ask the user whether the motors should drive in a square or float
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" Rising|Falling ", 0, 2);
			t.drawString("  edge | edge   ", 0, 3);
			t.drawString("       |        ", 0, 4);

			buttonChoice = Button.waitForAnyPress();


			if (buttonChoice == Button.ID_RIGHT) {
				odometer.start();
				
				new LocalisationManager(usPoller, colorPoller, nav, true).start();
				
				
				odometryDisplay.start();
			}
		}

		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
