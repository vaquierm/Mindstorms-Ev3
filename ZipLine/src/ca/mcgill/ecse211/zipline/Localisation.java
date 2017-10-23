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
		int currentTheta = (int) ZipLineLab.odometer.getThetaDegrees();
		int referenceHeadingCode = 0;
		currentX = getClosestMultiple(currentX);
		currentY = getClosestMultiple(currentY);
		currentTheta = getClosestReference(currentTheta);
		
		nav.turnTo(currentTheta);
		
		switch(currentTheta) {
		case 45:
			referenceHeadingCode = 3;
			break;
		case 135:
			referenceHeadingCode = 2;
			break;
		case 225:
			referenceHeadingCode = 0;
			break;
		case 315:
			referenceHeadingCode = 1;
		}
		
		
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		colorPoller.resumeThread(); 	//Need to detect lines, turn on color sensor
		ZipLineLab.leftMotor.setSpeed(ROTATION_SPEED);	//Start spinning in place
		ZipLineLab.rightMotor.setSpeed(ROTATION_SPEED);
		ZipLineLab.leftMotor.backward();
		ZipLineLab.rightMotor.forward();
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[referenceHeadingCode] = ZipLineLab.odometer.getThetaDegrees(); 	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[referenceHeadingCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[referenceHeadingCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[referenceHeadingCode] = ZipLineLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		ZipLineLab.leftMotor.stop(true);	//Four lines have now been detected. Stop spinning
		ZipLineLab.rightMotor.stop();
		
		colorPoller.stopPolling();	//No longer need color sensor. Turn off.
		ZipLineLab.odometer.setX(computeX(currentX));	//Use ComputeX() and ComputeY() to correct odometer's position
		ZipLineLab.odometer.setY(computeY(currentY));
		//ZipLineLab.odometer.setTheta(computeThetaColor());
	}
	
	//takes as input a heading in degrees and returns the closest heading at a 45 degree angle
	private int getClosestReference(int current) {
		if (current < 90)
			return 45;
		else if (current < 180)
			return 135;
		else if (current < 270)
			return 225;
		else
			return 315;
	}
	
	//return closest multiple of tile length
	private double getClosestMultiple(double val) {
		double mod = val % ZipLineLab.TILE;
		int intDiv = (int) (val / ZipLineLab.TILE);
		if (mod < ZipLineLab.TILE / 2) {
			return intDiv * ZipLineLab.TILE;
		} else
			return (intDiv + 1) * ZipLineLab.TILE;
	}
	
	//Method to compute X position with line data
	private double computeX(double targetX) {
		double thetaD = (lines[0] - lines[2]);
		int magnitude = -1;
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		if(thetaD > 180) {
			thetaD = 360 - thetaD;
			magnitude = 1;
		}
		thetaD/=2;
		double offset = Math.abs(COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD)));	//Formula for X coordinate, given in fourth quadrant
		
		return targetX + (magnitude * offset);
	}
	
	private double computeY(double targetY) {
		double thetaD = (lines[1] - lines[3]);
		int magnitude = -1;
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		if(thetaD > 180) {
			thetaD = 360 - thetaD;
			magnitude = 1;
		}
		thetaD/=2;
		double offset = Math.abs(COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD)));	//Formula for X coordinate, given in fourth quadrant
		
		return targetY + (magnitude * offset);	
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
