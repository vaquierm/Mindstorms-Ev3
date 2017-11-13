/**
 * ZiplineController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.Arrays;

import lejos.hardware.Sound;

/**
 * The ZipllineController is used to traverse the zipline. When running it, it is assumed that the robot is perfectly aligned with the zipline
 * and that all it has to do is to roll forward to mount it.
 * The association to the color poller is able to get data from the downward facing sensor to be able to know when the robot is off the ground.
 * After starting the wheels, the thread running here will be idle and will wait from an interrupt to indicate that the robot has "landed"
 * After landing, the Odometer is updated accordingly by looking at the values from the GameParameters.
 * @author Michael Vaquier
 *
 */

public class ZiplineLightData {
	
	//associations
	private ZiplineController ziplineController;
	
	private static final int DIFFERENCE_THRESHOLD = 2;
	private static final int DIFFERENCE_POINTS_COUNTER = 600;
	private static final int SAMPLE_POINTS = 10;
	private int differenceCounter = DIFFERENCE_POINTS_COUNTER;
	private int[] samplePoints;
	private int counter = 0;
	private double lastAverage = -1;
	private boolean tookOff = false;
	
	private long startTime = -1;
	
	/**
	 * Constructs an instance of the ZiplineLightData class.
	 */
	public ZiplineLightData() {
		samplePoints = new int[SAMPLE_POINTS];
		Arrays.fill(samplePoints, -1);
	}
	
	/**
	 * Takes as an input data coming from the color poller and processes it to detect is the robot has landed.
	 * When the detection is made, an interrupt is sent to the ziplineController instance it is associated to to indicate this event.
	 * @param newVal  New value fetched from the color sensor
	 */
	public void processData(int newVal) {
		if(samplePoints[counter] < 0) {
			samplePoints[counter] = newVal;
			lastAverage = newVal;
		} else {
			double newAverage = lastAverage + ((newVal - samplePoints[counter]) / SAMPLE_POINTS);
			double difference = 10 * Math.abs(newAverage - lastAverage);
			samplePoints[counter] = newVal;
			lastAverage = newAverage;
			if(tookOff) {
				if(difference > DIFFERENCE_THRESHOLD || System.currentTimeMillis() - startTime > 16000) {
					//The robot is landing
					Sound.beep();
					ziplineController.resumeThread();
					tookOff = false;
				}
			} else {
				if(difference < DIFFERENCE_THRESHOLD) {
					differenceCounter--;
				} else {
					differenceCounter = DIFFERENCE_POINTS_COUNTER;
				}
				
				if(differenceCounter < 0) {
					//The robot is now in the air.
					Sound.beep();
					tookOff = true;
					startTime = System.currentTimeMillis();
					ziplineController.resumeThread();
					Arrays.fill(samplePoints, -1);
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		counter = (counter + 1) % SAMPLE_POINTS;
	}
	
	/**
	 * Sets the ZiplineController association to this instance.
	 * @param ziplineController  Association to ZiplineController instance
	 */
	public void setZiplineController(ZiplineController ziplineController) {
		this.ziplineController = ziplineController;
	}
	
}
