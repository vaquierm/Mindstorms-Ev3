/**
 * UltrasonicLocalisationData.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.Arrays;

import lejos.hardware.Sound;

/**
 * The UltrasonoicLocationData class is used to process date from the
 * ultrasonic poller to detect edges
 * 
 * @author Michael Vaquier
 *
 */

public class UltrasonicLocalisationData {
	
	private Localisation localisation;
	
	private boolean fallingEdge = false;
	private boolean foundFirstEdge = false;
	
	private int[] samplePoints;
	private double lastAverage = -1;
	private static final int SAMPLE_POINTS = 5;
	private int counter = 0;
	private static final int EDGE_THRESHOLD = 50;
	
	/**
	 * Constructs an UltrasonicLocalisationData object.
	 */
	public UltrasonicLocalisationData() {
		samplePoints = new int[SAMPLE_POINTS];
		Arrays.fill(samplePoints, -1);
	}
	
	/**
	 * Processes the data coming from the ultrasonic poller and sends an interrupt to the localisation class when
	 * an edge is found. The process looks for falling and rising edges at the same time and sticks with one
	 * once the first edge is found.
	 * @param newVal  New value read from the Ultrasonic sensor
	 */
	public void processData(int newVal) {
		if(samplePoints[counter] < 0) {
			samplePoints[counter] = newVal;
			lastAverage = newVal;
		} else {
			double newAverage = lastAverage + ((newVal - samplePoints[counter]) / SAMPLE_POINTS);
			if (!foundFirstEdge) {
				/*if (lastAverage >= EDGE_THRESHOLD && newAverage <= EDGE_THRESHOLD) { //TODO fix for comp
					fallingEdge = true;
					foundFirstEdge = true;
					localisation.setFallingEdge(fallingEdge);
					Arrays.fill(samplePoints, -1);
					localisation.resumeThread();
					threadWait();
				} else */if (lastAverage <= EDGE_THRESHOLD && newAverage >= EDGE_THRESHOLD) {
					fallingEdge = false;
					foundFirstEdge = true;
					localisation.setFallingEdge(fallingEdge);
					Arrays.fill(samplePoints, -1);
					localisation.resumeThread();
					threadWait();
				}
			} else {
				if (fallingEdge && lastAverage >= EDGE_THRESHOLD && newAverage <= EDGE_THRESHOLD) {
					localisation.resumeThread();
					Arrays.fill(samplePoints, -1);
					foundFirstEdge = false;
					threadWait();
				} else if (!fallingEdge && lastAverage <= EDGE_THRESHOLD && newAverage >= EDGE_THRESHOLD) {
					localisation.resumeThread();
					Arrays.fill(samplePoints, -1);
					foundFirstEdge = false;
					threadWait();
				}
			}
			
			samplePoints[counter] = newVal;
			lastAverage = newAverage;
		}
		counter = (counter + 1) % SAMPLE_POINTS;
	}
	
	/**
	 * Sets the localisation association to the instance.
	 * @param localisation  Association to the new Localisation
	 */
	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}
	
	/**
	 * Puts the thread to sleep for one second.
	 */
	private void threadWait() {
		Sound.beep();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
		}
	}

}
