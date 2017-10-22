package ca.mcgill.ecse211.zipline;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 * Localisation outlines the procedure for both the US angle correction and color position correction
 * by using filtered data from the sensors to update the odometers accordingly
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class Localisation {
	
	private UltrasonicPoller usPoller;
	private ColorPoller colorPoller;
	private Navigation nav;
	private static final int ROTATION_SPEED = 150;
	
	private static final int COLOR_SENSOR_OFFSET = 15;
	
	private int tileIndexX = 0;
	private int tileIndexY = 0;
	
	private int tileCornerX = 0;
	private int tileCornerY = 0;
	
	private int cornerCode = 0;
	
	private static final int colorLocalisationLineOffset = 7;
	
	private boolean fallingEdge;
	
	public double edgeDifference = -1;
	
	private boolean waiting = false;
	private Object lock = new Object();
	
	private double[] edges = {-1, -1};
	private double[] lines = {-1, -1, -1, -1};
	//Localisation constructor that makes use of the US & Color sensor with the
	//UltrasonicPoller and ColorPoller objects
	public Localisation(UltrasonicPoller usPoller, ColorPoller colorPoller, Navigation nav, boolean fallingEdge) {
		this.usPoller = usPoller;
		this.fallingEdge = fallingEdge;
		this.colorPoller = colorPoller;
		this.nav = nav;
	}
	//Method outlining process for aligning to 0 deg. using an ultrasonic correction
	public void alignAngle() {
		usPoller.start();		//Start taking in US values
		ZipLineLab.leftMotor.setSpeed(ROTATION_SPEED);		
		ZipLineLab.rightMotor.setSpeed(ROTATION_SPEED);
		ZipLineLab.leftMotor.backward();			//Start spinning in place
		ZipLineLab.rightMotor.forward();
		setWaiting(true);		//start waiting until an edge value is read
		while(getWaiting()) {		//do nothing while waiting
			
		}
		edges[0] = ZipLineLab.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		ZipLineLab.leftMotor.forward();	//start spinning in opposite direction
		ZipLineLab.rightMotor.backward();
		setWaiting(true);	//start waiting until an edge value is read	
		while(getWaiting()) {	//do nothing while waiting
			
		}
		edges[1] = ZipLineLab.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		ZipLineLab.odometer.setTheta(Math.toRadians(computeAngle())); //Uses compute angle to set odometer's theta orientation
		ZipLineLab.leftMotor.stop(true);		//Two values have been found, stop spinning
		ZipLineLab.rightMotor.stop();
		usPoller.stopPolling();	//No longer need US sensor
		
	}
	//Method to Localise X,Y coordinates
	public void fixXY() {
		double currentX = ZipLineLab.odometer.getX();
		double currentY = ZipLineLab.odometer.getY();
		
		tileIndexX = (int) (currentX/ZipLineLab.TILE) + 1;
		tileIndexY = (int) (currentY/ZipLineLab.TILE) + 1;
		
		if (2 * tileIndexX / ZipLineLab.BOARD_SIZE <= 1) {
			tileCornerX = 1;
		}
		else {
			tileCornerX = 0;
		}
		
		if (2 * tileIndexY / ZipLineLab.BOARD_SIZE <= 1) {
			tileCornerY = 1;
		}
		else {
			tileCornerY = 0;
		}
		
		double destinationX;
		double destinationY;
		
		if (tileCornerX > 0) {
			destinationX = (tileIndexX * ZipLineLab.TILE) - colorLocalisationLineOffset;
		} else {
			destinationX = ((tileIndexX - 1) * ZipLineLab.TILE) + colorLocalisationLineOffset;
		}
		
		if (tileCornerY > 0) {
			destinationY = (tileIndexY * ZipLineLab.TILE) - colorLocalisationLineOffset;
		} else {
			destinationY = ((tileIndexY - 1) * ZipLineLab.TILE) + colorLocalisationLineOffset;
		}
		
		nav.travelTo(destinationX, destinationY, false);
		
		cornerCode = tileCornerY | (tileCornerX << 1);
		/*
		 * Corner code will be
		 * 0 for bottom left
		 * 1 for top left
		 * 2 for bottom right
		 * 3 for top right
		 */
		ZipLineLab.odometryDisplay.setDisplay(false);
		ZipLineLab.lcd.drawString(tileCornerX + " " + tileCornerY + " " + cornerCode, 0, 4);
		ZipLineLab.lcd.drawString(tileIndexX + " " + tileIndexY, 0, 5);
		ZipLineLab.lcd.drawString(destinationX + " " + destinationY, 0, 6);
		while(Button.waitForAnyPress() != Button.ID_ENTER);
		ZipLineLab.odometryDisplay.setDisplay(true);
		
		switch (cornerCode) {
		case 0:
			nav.turnTo(225);
			break;
		case 1:
			nav.turnTo(315);
			break;
		case 2:
			nav.turnTo(135);
			break;
		case 3:
			nav.turnTo(45);
		}
		
		cornerCode = (cornerCode + 1) % lines.length;
		colorPoller.resumeThread(); 	//Need to detect lines, turn on color sensor
		ZipLineLab.leftMotor.setSpeed(ROTATION_SPEED);	//Start spinning in place
		ZipLineLab.rightMotor.setSpeed(ROTATION_SPEED);
		ZipLineLab.leftMotor.backward();
		ZipLineLab.rightMotor.forward();
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[cornerCode] = ZipLineLab.odometer.getThetaDegrees(); 	//Once a line has been found add to lines array
		cornerCode = (cornerCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[cornerCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		cornerCode = (cornerCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[cornerCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		cornerCode = (cornerCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[cornerCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		ZipLineLab.leftMotor.stop(true);	//Four lines have now been detected. Stop spinning
		ZipLineLab.rightMotor.stop();
		
		colorPoller.stopPolling();	//No longer need color sensor. Turn off.
		ZipLineLab.odometer.setX(computeX());	//Use ComputeX() and ComputeY() to correct odometer's position
		ZipLineLab.odometer.setY(computeY());
		//ZipLineLab.odometer.setTheta(computeThetaColor());
	}
	//Method to compute X position with line data
	private double computeX() {
		double thetaD = (lines[0] - lines[2]);
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		thetaD/=2;
		double offset = Math.abs(COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD)));	//Formula for X coordinate, given in fourth quadrant
		double out = 0;
		switch (tileCornerX) {
		case 0:
			out = ((tileIndexX - 1) * ZipLineLab.TILE) + offset;
			break;
		case 1:
			out = (tileIndexX * ZipLineLab.TILE) - offset;
			break;
		default:
			break;
		}
		return out;	
	}
	
	private double computeY() {
		double thetaD = (lines[1] - lines[3]);
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		thetaD /=2;
		double offset = Math.abs(COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD)));	//Formula for X coordinate, given in fourth quadrant
		double out = 0;
		switch (tileCornerY) {
		case 0:
			out = ((tileIndexY - 1) * ZipLineLab.TILE) + offset;
			break;
		case 1:
			out = (tileIndexY * ZipLineLab.TILE) - offset;
			break;
		default:
			break;
		}
		return out;	
	}
	//Method to compute Y position with line data
	private double computeAngle() {
		double heading = edges[1] - edges[0];
		if (heading < 0) {	//Corrects for negative difference
			heading += 360;
		}
		edgeDifference = heading;
		heading /= 2;
		int base = 0;
		switch (ZipLineLab.corner) {
		case "0":
			if(!fallingEdge) {
				base = 225;
			} else {
				base = 45;
			}
			break;
		case "1":
			if(!fallingEdge) {
				base = 135;
			} else {
				base = 315;
			}
			break;
		case "2":
			if(!fallingEdge) {
				base = 45;
			} else {
				base = 225;
			}
			break;
		case "3":
			if(!fallingEdge) {
				base = 315;
			} else {
				base = 135;
			}
			break;
		}
		if (!fallingEdge) { // if rising edge
			heading = base + heading; //formula for orientation
		}
		else {	// if falling edge
			heading = base + heading;	//formula for orientation
		}
		return heading;
	}
	
	private double computeThetaColor() {

		return Math.toRadians(270+((lines[2] - lines[0])/2));
	}
	
	
	//These two methods allow for methods to wait till color/US sensors
	//find edge data (edge of wall/line) to store in arrays before using that
	//data to calculate a correction
	public boolean getWaiting() {
		synchronized(lock) {
			return waiting;
		}
	}
	
	public void setWaiting(boolean b) {
		synchronized(lock) {
			waiting = b;
		}
	}

}
