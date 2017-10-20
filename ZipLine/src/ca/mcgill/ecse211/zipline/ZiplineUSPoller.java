package ca.mcgill.ecse211.zipline;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * 
 *
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class ZiplineUSPoller extends Thread {

	
	private boolean polling = true;
	private Object lock = new Object();
	
	private SampleProvider us;
	private float[] usData;

	private int[] sensorData; // This array keeps the last SAMPLE_POINT points
								// of the us sensor
	private static final int SAMPLE_POINTS = 5;
	private int dataCounter = 0;
	private int currentAverage = -1;


	private static final int THRESHOLD = 17;

	private ZiplineController ziplineController;

	public ZiplineUSPoller(SampleProvider us, float[] usData) {
		this.us = us;
		this.usData = usData;
		
		this.sensorData = new int[SAMPLE_POINTS];
		for (int i = 0; i < SAMPLE_POINTS; i ++) {
			this.sensorData[i] = -1;
		}
	}

	/*
	 * This method starts the poling process of the us sensor
	 * The polling stops when the polling boolean is accessed by another thread to terminate this process
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		int distance;
		synchronized(lock) {
			polling = true;
		}
		while (getPolling()) {
			us.fetchSample(usData, 0); // acquire data
			distance = (int) (usData[0] * 100.0); // extract from buffer, cast
													// to int
			if (distance > 0) {
				newSensorValue(distance);
				if(checkThreshold()) {
					Sound.beep();
					ziplineController.setWaiting(false);
				}
			}

			try {
				Thread.sleep(50);
			} catch (Exception e) {
			} // Poor man's timed sampling
		}
		return;
	}

	/*
	 * This method is used to check if the current and past data indicates the presence of an edge.
	 */
	private boolean checkThreshold() {
		return (currentAverage < THRESHOLD);
	}

	private void newSensorValue(int newVal) {

		if (dataCounter < SAMPLE_POINTS) {
			sensorData[dataCounter] = newVal;
			currentAverage = newVal; // for the first few points the average is
									// just the last value.
			dataCounter++;
		} else {
			int newAverageSum =0;
			for (int i = 0; i < SAMPLE_POINTS; i++) {
				if (i == SAMPLE_POINTS - 1) {
					sensorData[i] = newVal;
					newAverageSum+=newVal;
				} else {
					sensorData[i] = sensorData[i + 1];
					newAverageSum+=sensorData[i + 1];
				}
			}
			currentAverage = newAverageSum / SAMPLE_POINTS;
		}
	}

	
	
	public void stopPolling() {
		synchronized(lock) {
			polling = false;
		}
	}
	
	private boolean getPolling() {
		synchronized(lock) {
			return polling;
		}
	}
	
	public void setController(ZiplineController ziplineController) {
		this.ziplineController = ziplineController;
	}

}
