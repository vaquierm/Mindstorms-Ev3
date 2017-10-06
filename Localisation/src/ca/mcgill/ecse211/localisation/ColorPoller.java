/*
 * ColorPoller.java
 */
package ca.mcgill.ecse211.localisation;

import lejos.hardware.Sound;
import lejos.robotics.SampleProvider;

/**
 * 
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class ColorPoller extends Thread {
	
	private static final int OFFSET = 6; //distance between sensor and center of the robot. (Sensor is behind the center)
	private static final int SQUARE_SIDE = 30; //The square side (we used an integer for faster calculation)
	
	
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private SampleProvider colorRed;
	private float[] colorRedData; //array used to fetch the sensor data
	

	private static final int SAMPLE_POINTS = 5; //length of shift register used for the sensor data
	private int dataCounter = 0; //index used for filling the register at the start
	private int[] sensorData; //Shift register used for sensor data

	private static final int DIFFERENCE_THRESHOLD = 40; //Threshold of the pulse seen in the difference grapf for it to be a line
	private static final int DIFFERENCE_SCALING = 11; //Scaling constant
	private static final int DIFFERENCE_POINTS = 10; //length of shift register used for derivative data
	private int differenceCounter = 0; //index used for several first points
	private int lastAverage = -1; //last average used to calculate the new average.
	private int[] differenceData; //shift register used for derivative data
	
	private boolean polling = false;
	private Object lock = new Object();
	
	private Localisation localisation;

	// constructor
	public ColorPoller(Odometer odometer, SampleProvider colorRed, float[] colorRedData) {
		this.odometer = odometer;
		this.colorRed = colorRed;
		this.colorRedData = colorRedData;
	
		sensorData = new int[SAMPLE_POINTS];
		for (int i = 0; i < SAMPLE_POINTS; i++) { // initialize all entries to
													// -1
			sensorData[i] = -1;
		}

		differenceData = new int[DIFFERENCE_POINTS];
		for (int i = 0; i < DIFFERENCE_POINTS; i++) { // initialize all entries
														// to
			// -1
			differenceData[i] = -1;
		}
	}

	// run method (required for Thread)
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
			
			if(isOnLine()) {
				localisation.setWaiting(false);
				Sound.beep();
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

	private void newDifferenceValue(int newVal) {
		//System.out.println(newVal + ", " + odometer.getX()+ ", " + odometer.getY());
		if (differenceCounter < DIFFERENCE_POINTS) {
			differenceData[differenceCounter] = newVal;
			differenceCounter++;
		} else {
			for (int i = 0; i < DIFFERENCE_POINTS; i++) {
				if (i == DIFFERENCE_POINTS - 1) {
					differenceData[i] = newVal;
				} else {
					differenceData[i] = differenceData[i + 1];
				}
			}
		}
	}

	private void newAverageValue(int newVal) {
		int newAverage = lastAverage + ((newVal - sensorData[0]) / SAMPLE_POINTS);
		//System.out.println("[" + sensorData[0] + ", " + sensorData[1] + ", " + sensorData[2] + ", " + sensorData[3] + ", " + sensorData[4]+ "]");
		//System.out.println("new Average : "+ newAverage);
		newDifferenceValue((newAverage - lastAverage) * DIFFERENCE_SCALING);
		lastAverage = newAverage;
	}
	
	private boolean isOnLine() {
		boolean result;
		int min = 0;
		int max = 0;
		for(int i = 0; i < DIFFERENCE_POINTS; i++) {
			if (differenceData[i] < min) {
				min = differenceData[i];
			}
			if (differenceData[i] > max) {
				max = differenceData[i];
			}
		}
		result = min < -DIFFERENCE_THRESHOLD && max > DIFFERENCE_THRESHOLD;
		//System.out.println(result);
		if(result) {
			for (int i = 0 ; i < DIFFERENCE_POINTS ; i++) {
				if (differenceData[i] < -DIFFERENCE_THRESHOLD || differenceData[i] > DIFFERENCE_THRESHOLD) {
					differenceData[i] = 0;
				}
			}
		}
		return result;
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
	
	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}
	
}
