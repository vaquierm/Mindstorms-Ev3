/**
 * Localisation.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * Localisation outlines the procedure for both the US angle correction and color position correction
 * by using filtered data from the sensors to update the odometers accordingly
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class Localisation {
	
	private static final int ROTATION_SPEED = 150;
	private static final int COLOR_SENSOR_OFFSET = 15;
	
	private boolean fallingEdge = false;
	public double edgeDifference = -1;
	
	private volatile boolean paused = false;
	private Object pauseLock = new Object();
	
	private double[] edges = {-1, -1};
	private double[] lines = {-1, -1, -1, -1};
	//Localisation constructor that makes use of the US & Color sensor with the
	//UltrasonicPoller and MainController.colorLocalisationPoller objects
	public Localisation() {
		
	}
	
	//Procedure and logic of ultrasonic localisation routine
	public void usLocalisation() {
		MainController.usLocalisationPoller.start();		//Start taking in US values
		MainController.leftMotor.setSpeed(ROTATION_SPEED);		
		MainController.rightMotor.setSpeed(ROTATION_SPEED);
		MainController.leftMotor.backward();			//Start spinning in place
		MainController.rightMotor.forward();
		pauseThread();
		
		edges[0] = MainController.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		MainController.leftMotor.forward();	//start spinning in opposite direction
		MainController.rightMotor.backward();
		pauseThread();
		
		edges[1] = MainController.odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		MainController.odometer.setTheta(Math.toRadians(computeAngle())); //Uses compute angle to set odometer's theta orientation
		MainController.leftMotor.stop(true);		//Two values have been found, stop spinning
		MainController.rightMotor.stop();
		MainController.usLocalisationPoller.stopPolling();	//No longer need US sensor
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}
	
	//Procedure and logic of color loclisation routine
	public void colorLocalisation() {
		
		MainController.colorLocalisationPoller.start(); 	//Need to detect lines, turn on color sensor
		MainController.leftMotor.setSpeed(ROTATION_SPEED);	//Start spinning in place
		MainController.rightMotor.setSpeed(ROTATION_SPEED);
		MainController.leftMotor.backward();
		MainController.rightMotor.forward();
		
		pauseThread();
		lines[0] = MainController.odometer.getThetaDegrees(); 	//Once a line has been found add to lines array
		pauseThread();
		lines[2] = MainController.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		pauseThread();
		lines[1] = MainController.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		pauseThread();
		lines[3] = MainController.odometer.getThetaDegrees();	//Once a line has been found add to lines array
		
		MainController.leftMotor.stop(true);	//Four lines have now been detected. Stop spinning
		MainController.rightMotor.stop();
		
		MainController.colorLocalisationPoller.stopPolling();	//No longer need color sensor. Turn off.
		MainController.odometer.setX(computeX());	//Use ComputeX() and ComputeY() to correct odometer's position
		MainController.odometer.setY(computeY());
		MainController.odometer.setTheta(computeThetaColor());
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
	
	private double computeThetaColor() {
		return Math.toRadians(275+(lines[2] - lines[3]));
	}
	
	
	//pauses the thread
	private void pauseThread() {
		paused = true;
		synchronized (pauseLock) {
            if (paused) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
	}
	
	//resumes the thread
    public void resumeThread() {
        synchronized (pauseLock) {
        	if (paused) {
        		paused = false;
        		pauseLock.notifyAll(); // Unblocks thread
        	}
        }
    }

}
