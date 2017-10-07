package ca.mcgill.ecse211.localisation;

/**
 * Localisation outlines the procedure for both the US angle correction and color position correction
 * by using filtered data from the sensors to update the odometers accordingly
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class Localisation {
	
	private UltrasonicPoller usPoller;
	private ColorPoller colorPoller;
	private static final int ROTATION_SPEED = 150;
	
	private static final int COLOR_SENSOR_OFFSET = 15;
	
	private boolean fallingEdge;
	
	public double edgeDifference = -1;
	
	private boolean waiting = false;
	private Object lock = new Object();
	
	private double[] edges = {-1, -1};
	private double[] lines = {-1, -1, -1, -1};
	//Localisation constructor that makes use of the US & Color sensor with the
	//UltrasonicPoller and ColorPoller objects
	public Localisation(UltrasonicPoller usPoller, ColorPoller colorPoller, boolean fallingEdge) {
		this.usPoller = usPoller;
		this.fallingEdge = fallingEdge;
		this.colorPoller = colorPoller;
	}
	//Method outlining process for aligning to 0 deg. using an ultrasonic correction
	public void alignAngle() {
		usPoller.start();		//Start taking in US values
		LocalisationLab.leftMotor.setSpeed(ROTATION_SPEED);		
		LocalisationLab.rightMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.leftMotor.backward();			//Start spinning in place
		LocalisationLab.rightMotor.forward();
		setWaiting(true);		//start waiting until an edge value is read
		while(getWaiting()) {		//do nothing while waiting
			
		}
		edges[0] = LocalisationLab.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		LocalisationLab.leftMotor.forward();	//start spinning in opposite direction
		LocalisationLab.rightMotor.backward();
		setWaiting(true);	//start waiting until an edge value is read	
		while(getWaiting()) {	//do nothing while waiting
			
		}
		edges[1] = LocalisationLab.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		LocalisationLab.odometer.setTheta(Math.toRadians(computeAngle())); //Uses compute angle to set odometer's theta orientation
		LocalisationLab.leftMotor.stop(true);		//Two values have been found, stop spinning
		LocalisationLab.rightMotor.stop();
		usPoller.stopPolling();	//No longer need US sensor
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}
	//Method to Localise X,Y coordinates
	public void fixXY() {
		
		colorPoller.start(); 	//Need to detect lines, turn on color sensor
		LocalisationLab.leftMotor.setSpeed(ROTATION_SPEED);	//Start spinning in place
		LocalisationLab.rightMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.leftMotor.backward();
		LocalisationLab.rightMotor.forward();
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[0] = LocalisationLab.odometer.getThetaDegrees(); 	//Once a line has been found add to lines array
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[2] = LocalisationLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[1] = LocalisationLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		setWaiting(true);	//Again waiting for a trigger value i.e a line
		while(getWaiting()) {
			
		}
		lines[3] = LocalisationLab.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		LocalisationLab.leftMotor.stop(true);	//Four lines have now been detected. Stop spinning
		LocalisationLab.rightMotor.stop();
		
		colorPoller.stopPolling();	//No longer need color sensor. Turn off.
		LocalisationLab.odometer.setX(computeX());	//Use ComputeX() and ComputeY() to correct odometer's position
		LocalisationLab.odometer.setY(computeY());
	}
	//Method to compute X position with line data
	private double computeX() {
		double thetaD = (lines[0] - lines[1])/2;
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		return -COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD));	//Formula for X coordinate, given in fourth quadrant
	}
	
	private double computeY() {
		double thetaD = (lines[2] - lines[3])/2;
		if(thetaD < 0) {	//Corrects for negative difference
			thetaD += 360;
		}
		return -COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD));	//Formula for X coordinate, given in fourth quadrant
	}
	//Method to compute Y position with line data
	private double computeAngle() {
		double heading = edges[1] - edges[0];
		if (heading < 0) {	//Corrects for negative difference
			heading += 360;
		}
		edgeDifference = heading;
		heading /= 2;
		if (!fallingEdge) { // if rising edge
			heading = 225 + heading; //formula for orientation
		}
		else {	// if falling edge
			heading = 45 + heading;	//formula for orientation
		}
		return heading;
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
