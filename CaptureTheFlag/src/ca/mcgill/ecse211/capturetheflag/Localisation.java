/**
 * Localisation.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

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
	
	//Game board constants
	private final double tile;
	private final int startingCorner;
	
	//Associations
	private Odometer odometer;
	private Navigation navigation;
	
	//Poller
	private UltrasonicPoller ultrasonicPoller;
	private ColorPoller colorPoller;
	
	//Motors
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	
	//Localisation constructor that makes use of the US & Color sensor with the
	//UltrasonicPoller and colorLocalisationPoller objects
	public Localisation(Odometer odometer, Navigation navigation, UltrasonicPoller ultrasonicPoller, ColorPoller colorPoller, EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, double tile, int startingCorner) {
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.odometer = odometer;
		this.navigation = navigation;
		this.ultrasonicPoller = ultrasonicPoller;
		this.colorPoller = colorPoller;
		this.tile = tile;
		this.startingCorner = startingCorner;
	}
	
	//Procedure and logic of ultrasonic localisation routine
	public void usLocalisation() {
		ultrasonicPoller.startPolling();		//Start taking in US values
		leftMotor.setSpeed(ROTATION_SPEED);		
		rightMotor.setSpeed(ROTATION_SPEED);
		leftMotor.backward();			//Start spinning in place
		rightMotor.forward();
		pauseThread();
		
		edges[0] = odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		leftMotor.forward();	//start spinning in opposite direction
		rightMotor.backward();
		pauseThread();
		
		edges[1] = odometer.getThetaDegrees();	//once an edge value is found, add to edges array
		odometer.setTheta(Math.toRadians(computeAngleUltrasonic())); //Uses compute angle to set odometer's theta orientation
		leftMotor.stop(true);		//Two values have been found, stop spinning
		rightMotor.stop();
		ultrasonicPoller.stopPolling();	//No longer need US sensor
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}
	
	//Procedure and logic of color loclisation routine
	public void colorLocalisation() {
		
		double currentX = odometer.getX();
		double currentY = odometer.getY();
		int currentTheta = (int) odometer.getThetaDegrees();
		int referenceHeadingCode = 0;
		currentX = getClosestMultiple(currentX);
		currentY = getClosestMultiple(currentY);
		currentTheta = getClosestReference(currentTheta);
		
		navigation.turnTo(currentTheta);
		
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
		colorPoller.startPolling(); 	//Need to detect lines, turn on color sensor
		leftMotor.setSpeed(ROTATION_SPEED);	//Start spinning in place
		rightMotor.setSpeed(ROTATION_SPEED);
		leftMotor.backward();
		rightMotor.forward();
		pauseThread();
		
		lines[referenceHeadingCode] = odometer.getThetaDegrees(); 	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		pauseThread();
		
		lines[referenceHeadingCode] = odometer.getThetaDegrees();	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		pauseThread();
		
		lines[referenceHeadingCode] = odometer.getThetaDegrees();	//Once a line has been found add to lines array
		referenceHeadingCode = (referenceHeadingCode + 1) % lines.length;
		pauseThread();
		
		lines[referenceHeadingCode] = odometer.getThetaDegrees();	//Once a line has been found add to lines array
		leftMotor.stop(true);	//Four lines have now been detected. Stop spinning
		rightMotor.stop();
		
		colorPoller.stopPolling();	//No longer need color sensor. Turn off.
		odometer.setX(computeX(currentX));	//Use ComputeX() and ComputeY() to correct odometer's position
		odometer.setY(computeY(currentY));
		double newT = computeThetaColor(referenceHeadingCode);

		odometer.setTheta(Math.toRadians(newT));
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
	
	//Method to compute Y position with line data
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
	
	//Method to compute Theta position with line data
	private double computeThetaColor(int reference) {
		double delta;
		double out = 0;
		//odometryDisplay.setDisplay(false);
		switch(reference) {
			case 3:
				delta = lines[1] - lines[3];
				if(delta < 0)
					delta += 360;
				delta /= 2;
				out = 180 - delta;
				break;
			case 0:
				delta = lines[1] - lines[3];
				if(delta < 0)
					delta += 360;
				delta /= 2;
				out = 180 + delta;
				break;
			case 1:
				delta = lines[0] - lines[2];
				if(delta < 0)
					delta += 360;
				delta /= 2;
				//lcd.drawString(delta + "", 0, 5);
				out = 270 + delta;
				break;
			case 2:
				delta = lines[0] - lines[2];
				if(delta < 0)
					delta += 360;
				delta /= 2;
				//lcd.drawString(delta + "", 0, 5);
				out = 270 - delta;
				break;
		}
		if (out >= 360)
			out -= 360;
		else if (out < 0) {
			out += 360;
		}
		return out;
	}
	
	//Method to compute Theta position with edge data
	private double computeAngleUltrasonic() {
		double heading = edges[1] - edges[0];
		if (heading < 0) {	//Corrects for negative difference
			heading += 360;
		}
		edgeDifference = heading;
		heading /= 2;
		int base = 0;
		switch (startingCorner) {
		case 0:
			if(!fallingEdge) {
				base = 225;
			} else {
				base = 45;
			}
			break;
		case 1:
			if(!fallingEdge) {
				base = 135;
			} else {
				base = 315;
			}
			break;
		case 2:
			if(!fallingEdge) {
				base = 45;
			} else {
				base = 225;
			}
			break;
		case 3:
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
		double mod = val % tile;
		int intDiv = (int) (val / tile);
		if (mod < tile / 2) {
			return intDiv * tile;
		} else
			return (intDiv + 1) * tile;
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
