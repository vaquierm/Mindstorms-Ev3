/*
 * ColorPoller.java
 */
package ca.mcgill.ecse211.zipline;

import lejos.hardware.Sound;
import lejos.robotics.SampleProvider;

/**
 * The zipline color poller fetches data from a color sensor and processes the data to determine the robot is off the ground or nor.
 * If a line is detected a flag variable in the localisation class is accessed to indicate that a line was found.
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class ZiplineColorPoller extends Thread {

	
	private static final long CORRECTION_PERIOD = 10;
	private SampleProvider colorRed;
	private float[] colorRedData; //array used to fetch the sensor data
	

	private static final int SAMPLE_POINTS = 10; //length of shift register used for the sensor data
	private int dataCounter = 0; //index used for filling the register at the start
	private int[] sensorData; //Shift register used for sensor data

	private static final int LIGHT_THRESHOLD = 2; 
	private int lastAverage = -1; //last average used to calculate the new average.
	private int currentAverage = -1;
	
	private boolean polling = false;
	private Object lock = new Object();
	
	private ZiplineController ziplineContoller;

	// constructor
	public ZiplineColorPoller(SampleProvider colorRed, float[] colorRedData) {
		this.colorRed = colorRed;
		this.colorRedData = colorRedData;
	
		sensorData = new int[SAMPLE_POINTS];
		for (int i = 0; i < SAMPLE_POINTS; i++) { // initialize all entries to								// -1
			sensorData[i] = -1;
		}

	}

	/*
	 * run method (required for Thread)
	 * This method periodically fetches data from the color sensor and processes it.
	 * This thread can be killed when the polling boolean is accessed form another thread.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		long correctionStart, correctionEnd;
		synchronized(lock) {
			polling = true;
		}
		while (getPolling()) {
			correctionStart = System.currentTimeMillis();

			colorRed.fetchSample(colorRedData, 0); // acquire data
			int sample = (int) (colorRedData[0] * 100); // extract from
															// buffer, cast to
															// int
			//System.out.println(sample);
			newSensorValue(sample);
			
			if(sendSignal()) {
				ziplineContoller.setWaiting(false);
				Sound.beep();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			
			
			

			// this ensure the odometry correction occurs only once every period
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < CORRECTION_PERIOD) {
				try {
					Thread.sleep(CORRECTION_PERIOD - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometry correction will be
					// interrupted by another thread
				}
			}
		}
		return;
	}
	

	/*
	 * the two next methods adds a new value in the shift register If it is
	 * full, the first value is erased and everything else is shifted. the new
	 * value is inserted in the last position.
	 */
	private void newSensorValue(int newVal) {

		if (dataCounter < SAMPLE_POINTS) {
			sensorData[dataCounter] = newVal;
			lastAverage = newVal; // for the first few points the average is
									// just the last value.
			dataCounter++;
		} else {
			newAverageValue(newVal);
			for (int i = 0; i < SAMPLE_POINTS; i++) {
				if (i == SAMPLE_POINTS - 1) {
					sensorData[i] = newVal;
				} else {
					sensorData[i] = sensorData[i + 1];
				}
			}
		}
	}


	private void newAverageValue(int newVal) {
		int newAverage = lastAverage + ((newVal - sensorData[0]) / SAMPLE_POINTS);
		lastAverage = currentAverage;
		//System.out.println("[" + sensorData[0] + ", " + sensorData[1] + ", " + sensorData[2] + ", " + sensorData[3] + ", " + sensorData[4]+ "]");
		//System.out.println("new Average : "+ newAverage);
		currentAverage = newAverage;
	}
	
	public boolean sendSignal() {
		if((currentAverage < LIGHT_THRESHOLD && lastAverage >= LIGHT_THRESHOLD) || (lastAverage < LIGHT_THRESHOLD && currentAverage >= LIGHT_THRESHOLD))
			return true;
		return false;
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
	
	
	
}
