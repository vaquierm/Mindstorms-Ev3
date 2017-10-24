// Lab5.java

package ca.mcgill.ecse211.zipline;

import java.util.LinkedList;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;

/**
 * 
 * ZipLineLab is where the robot's display is set up and where the sensors and
 * motors are set up
 * 
 * @author
 * 
 */
public class ZipLineLab {

	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));

	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));

	private static final lejos.hardware.port.Port colorPort = LocalEV3.get().getPort("S2");
	private static final lejos.hardware.port.Port usPort = LocalEV3.get().getPort("S1");

	public static Odometer odometer;
	
	public static String corner;

	public static NavigationController navigationController;

	public static LocalisationManager localisationManager;

	public static final int BOARD_SIZE = 8;
	public static final double TILE = 30.48;
	public static final double WHEEL_RADIUS = 2.1;
	public static final double TRACK = 16.6;
	
	public static TextLCD lcd;
	public static OdometryDisplay odometryDisplay;
	

	public static void main(String[] args) {

		lcd = LocalEV3.get().getTextLCD();
		odometer = new Odometer(leftMotor, rightMotor);
		odometryDisplay = new OdometryDisplay(odometer, lcd);

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
																		// is
																		// the
																		// buffer
																		// in
																		// which
																		// data
																		// are
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
		
		float[] usZiplineData = new float[meanFilterUs.sampleSize()];

		ColorPoller colorPoller = new ColorPoller(odometer, colorRedMean, colorRedData);
		colorPoller.start();

		UltrasonicPoller usPoller = new UltrasonicPoller(meanFilterUs, usData);

		Navigation navigation = new Navigation(odometer, leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);

		localisationManager = new LocalisationManager(usPoller, colorPoller, navigation, false);

		navigationController = new NavigationController(usPoller, odometer, navigation, new LinkedList<Coordinate>());

		ZiplineUSPoller ziplineUSPoller = new ZiplineUSPoller(meanFilterUs, usZiplineData);

		ZiplineController ziplineController = new ZiplineController(ziplineUSPoller);

		// This code can be used to test the wheel base of the robot
		/*
		 * leftMotor.setAcceleration(250); rightMotor.setAcceleration(250);
		 * leftMotor.setSpeed(200); rightMotor.setSpeed(200);
		 * leftMotor.rotate(nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);
		 * rightMotor.rotate(-nav.convertAngle(WHEEL_RADIUS, TRACK, 360), true);
		 */


		lcd.clear();
		lcd.drawString("Which corner is ", 0, 0);
		lcd.drawString("the robot       ", 0, 1);
		lcd.drawString("starting at?    ", 0, 2);
		lcd.drawString("       0        ", 0, 3);
		int id = Button.waitForAnyPress();
		corner = "0";
		String draw = "";
		while (id != Button.ID_ENTER) {
			switch(id) {
			case Button.ID_DOWN:
				draw = "       0        ";
				corner = "0";
				break;
			case Button.ID_UP:
				draw = "       1        ";
				corner = "1";
				break;
			case Button.ID_LEFT:
				draw = "       2        ";
				corner = "2";
				break;
			case Button.ID_RIGHT:
				draw = "       3        ";
				corner = "3";
				break;
			default:
				break;
			}
			lcd.drawString(draw, 0, 3);
			id = Button.waitForAnyPress();
		}
		
		lcd.clear();
		lcd.drawString("   x0  |    y0  ", 0, 0);
		lcd.drawString("-------|--------", 0, 1);
		lcd.drawString("   0   |    0   ", 0, 2);
		lcd.drawString("       |        ", 0, 3);

		int x = 0;
		int y = 0;
		id = Button.waitForAnyPress();
		while (id != Button.ID_ENTER) {
			if (id == Button.ID_LEFT) {
				x = (x - 1) % 13;
				if (x < 0)
					x += 13;
			} else if (id == Button.ID_RIGHT) {
				x = (x + 1) % 13;
			} else if (id == Button.ID_UP) {
				y = (y + 1) % 13;
			} else if (id == Button.ID_DOWN) {
				y = (y - 1) % 13;
				if (y < 0)
					y += 13;
			}
			draw = "   " + Integer.toString(x);
			if (x > 9) {
				draw += "  |    ";
			} else {
				draw += "   |    ";
			}
			draw += Integer.toString(y) + "  ";
			lcd.drawString(draw, 0, 2);
			id = Button.waitForAnyPress();
		}
		
		lcd.clear();
		lcd.drawString("   xc  |    yc  ", 0, 0);
		lcd.drawString("-------|--------", 0, 1);
		lcd.drawString("   0   |    0   ", 0, 2);
		lcd.drawString("       |        ", 0, 3);

		int xc = 0;
		int yc = 0;
		id = Button.waitForAnyPress();
		while (id != Button.ID_ENTER) {
			if (id == Button.ID_LEFT) {
				xc = (xc - 1) % 13;
				if (xc < 0)
					xc += 13;
			} else if (id == Button.ID_RIGHT) {
				xc = (xc + 1) % 13;
			} else if (id == Button.ID_UP) {
				yc = (yc + 1) % 13;
			} else if (id == Button.ID_DOWN) {
				yc = (yc - 1) % 13;
				if (yc < 0)
					yc += 13;
			}
			draw = "   " + Integer.toString(xc);
			if (xc > 9) {
				draw += "  |    ";
			} else {
				draw += "   |    ";
			}
			draw += Integer.toString(yc) + "  ";
			lcd.drawString(draw, 0, 2);
			id = Button.waitForAnyPress();
		}

		Coordinate initialLocalisation = new Coordinate(-1, -1);
		odometryDisplay.start();
		switch(corner) {
		case "0":
			odometer.setX(20);
			odometer.setY(20);
			initialLocalisation.x = TILE;
			initialLocalisation.y = TILE;
			break;
		case "1":
			odometer.setX((BOARD_SIZE * TILE) - 20);
			odometer.setY(20);
			initialLocalisation.x = (BOARD_SIZE - 1) * TILE;
			initialLocalisation.y = TILE;
			break;
		case "2":
			odometer.setX((BOARD_SIZE * TILE) - 20);
			odometer.setY((BOARD_SIZE * TILE) - 20);
			initialLocalisation.x = (BOARD_SIZE - 1) * TILE;
			initialLocalisation.y = (BOARD_SIZE - 1) * TILE;
			break;
		case "3":
			odometer.setX(20);
			odometer.setY((BOARD_SIZE * TILE) - 20);
			initialLocalisation.x = TILE;
			initialLocalisation.y = (BOARD_SIZE - 1) * TILE;
		}
		
		
		/*odometer.setX(15);
		odometer.setY(75);
		localisationManager.getLocalisation().fixXY();*/

		
		localisationManager.getLocalisation().alignAngle();
		navigation.travelTo(initialLocalisation.x, initialLocalisation.y, false);
		
		localisationManager.getLocalisation().fixXY();
		
		navigation.travelTo(x * TILE, y * TILE, false);
		//navigation.travelTo(x * TILE, y * TILE, false);
		
		
		localisationManager.getLocalisation().fixXY();
		
		
		
		navigation.travelTo(x * TILE, y * TILE, false);
		navigation.turnTo(xc * TILE, yc * TILE);

		while (Button.waitForAnyPress() != Button.ID_ENTER)
			;

		ziplineController.start();
		

		while (Button.waitForAnyPress() != Button.ID_ESCAPE)
			;
		System.exit(0);
	}
}
