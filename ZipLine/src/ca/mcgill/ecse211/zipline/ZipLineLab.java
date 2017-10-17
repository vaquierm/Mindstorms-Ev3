// Lab5.java

package ca.mcgill.ecse211.zipline;


import java.util.LinkedList;

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
 * ZipLineLab is where the robot's display is set up and where
 * the sensors and motors are set up
 * 
 * @author 
 * 
 */
public class ZipLineLab {

	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));

	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));

	private static final Port colorPort = LocalEV3.get().getPort("S2");
	private static final Port usPort = LocalEV3.get().getPort("S1");

	public static Odometer odometer;
	
	public static NavigationController navigationController;
	
	public static LocalisationManager localisationManager;


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
		
		UltrasonicPoller usPoller = new UltrasonicPoller(meanFilterUs, usData);
		
		Navigation navigation = new Navigation(odometer, leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
		
		localisationManager = new LocalisationManager(usPoller, colorPoller, navigation, false);
		
		navigationController = new NavigationController(usPoller, odometer, navigation, new LinkedList<Coordinate>());
		
		ZiplineController ziplineController = new ZiplineController();


		//This code can be used to test the wheel base of the robot
		/*leftMotor.setAcceleration(250);
		rightMotor.setAcceleration(250);
		leftMotor.setSpeed(200);
		rightMotor.setSpeed(200);
		leftMotor.rotate(nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);
		rightMotor.rotate(-nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);*/
		
		t.clear();
		t.drawString("  Press on the  ", 0, 0);
		t.drawString("middle button to", 0, 1);
		t.drawString(" localize then  ", 0, 2);
		t.drawString("ride the zipline", 0, 3);
		
		while (Button.waitForAnyPress() != Button.ID_ENTER)
			;
		
		//localisationManager.start();
		
		while (Button.waitForAnyPress() != Button.ID_ENTER)
			;
		
		odometryDisplay.setDisplay(false);
		
		t.clear();
		t.drawString("   x   |    y   ", 0, 0);
		t.drawString("-------|--------", 0, 1);
		t.drawString("   0   |    0   ", 0, 2);
		t.drawString("       |        ", 0, 3);
		
		int x = 0;
		int y = 0;
		int id = Button.waitForAnyPress();
		String draw;
		while (id != Button.ID_ENTER) {
			if (id == Button.ID_LEFT) {
				x = (x - 1)%13;
				if (x < 0)
					x += 13;
			}
			else if (id == Button.ID_RIGHT) {
				x = (x + 1)%13;
			}
			else if(id == Button.ID_UP) {
				y = (y + 1)%13;
			}
			else if(id == Button.ID_DOWN) {
				y = (y - 1)%13;
				if (y < 0)
					y += 13;
			}
			draw = "   " + Integer.toString(x);
			if(x > 9) {
				draw += "  |    ";
			}
			else {
				draw += "   |    ";
			}
			draw += Integer.toString(y);
			t.drawString(draw, 0, 2);
			id = Button.waitForAnyPress();
		}
		
		odometryDisplay.setDisplay(true);
		
		navigationController.addWayPoint(new Coordinate(x, y));
		navigationController.start();
		
		while (Button.waitForAnyPress() != Button.ID_ENTER)
			;
		
		ziplineController.start();

		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
