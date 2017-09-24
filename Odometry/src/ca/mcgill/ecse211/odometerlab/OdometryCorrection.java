/*
 * OdometryCorrection.java
 */
package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.Sound;
import lejos.robotics.SampleProvider;

public class OdometryCorrection extends Thread {
	
	private static final float OFFSET = 5;
	private static final int ANGLE_TOLERANCE = 1;
	private static final int DISTANCE_TOLERANCE = 8;
	private static final int SQUARE_SIDE = 30;
	
	
	private static final long CORRECTION_PERIOD = 10;
	private Odometer odometer;
	private SampleProvider colorRed;
	private float[] colorRedData;
	

	private static final int SAMPLE_POINTS = 5;
	private int dataCounter = 0;
	private int[] sensorData;

	private static final int DIFFERENCE_THRESHOLD = 40;
	private static final int DIFFERENCE_SCALING = 11;
	private static final int DIFFERENCE_POINTS = 7;
	private int differenceCounter = 0;
	private int lastAverage = -1;
	private int[] differenceData;
	
	//this variable is used to track what correction was made last such that it
	//does not correct multiple times and make the position even more innacurate.
	private int lastCorrection = -1;

	// constructor
	public OdometryCorrection(Odometer odometer, SampleProvider colorRed, float[] colorRedData) {
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

		while (true) {
			correctionStart = System.currentTimeMillis();

			colorRed.fetchSample(colorRedData, 0); // acquire data
			int sample = (int) (colorRedData[0] * 100); // extract from
															// buffer, cast to
															// int
			//System.out.println(sample);
			newSensorValue(sample);
			
			if(differenceCounter == DIFFERENCE_POINTS && isOnLine()) {
				double theta = odometer.getThetaDegrees();
				double X = odometer.getX();
				double Y = odometer.getY();
				int thisCorrection;
				int closestMultiple;
				//System.out.println("("+X+", "+Y+")");
				
				if ((theta <= ANGLE_TOLERANCE || theta > 360 - ANGLE_TOLERANCE) && (Math.abs((Y - OFFSET) % SQUARE_SIDE) <= DISTANCE_TOLERANCE || Math.abs((Y - OFFSET + DISTANCE_TOLERANCE) % SQUARE_SIDE) <= DISTANCE_TOLERANCE)) { //traveling in +Y
					closestMultiple = closestMultipleOfSquareSide(Y);
					thisCorrection = closestMultiple + 1;
					if(thisCorrection != lastCorrection) {
						//System.out.println("("+X+", "+Y+") +Y");
						odometer.setY(closestMultiple + OFFSET);
						//System.out.println("("+odometer.getX()+", "+odometer.getY()+") +Y");
						Sound.beep();
						lastCorrection = thisCorrection;
					}
				}
				else if (theta <= 180 + ANGLE_TOLERANCE && theta >= 180 - ANGLE_TOLERANCE && (Math.abs((Y + OFFSET) % SQUARE_SIDE) <= DISTANCE_TOLERANCE || Math.abs((Y + OFFSET + DISTANCE_TOLERANCE) % SQUARE_SIDE) <= DISTANCE_TOLERANCE)) { //traveling in -Y
					closestMultiple = closestMultipleOfSquareSide(Y);
					thisCorrection = closestMultiple + 2;
					if(thisCorrection != lastCorrection) {
						//System.out.println("("+X+", "+Y+") -Y");
						odometer.setY(closestMultiple - OFFSET);
						//System.out.println("("+odometer.getX()+", "+odometer.getY()+") -Y");
						Sound.beep();
					}
				}
				else if (theta <= 90 + ANGLE_TOLERANCE && theta >= 90 - ANGLE_TOLERANCE && (Math.abs((X - OFFSET) % SQUARE_SIDE) <= DISTANCE_TOLERANCE || Math.abs((X - OFFSET + DISTANCE_TOLERANCE) % SQUARE_SIDE) <= DISTANCE_TOLERANCE)) { //traveling in +X
					closestMultiple = closestMultipleOfSquareSide(Y);
					thisCorrection = closestMultiple + 3;
					if(thisCorrection != lastCorrection) {
						//System.out.println("("+X+", "+Y+") +X");
						odometer.setY(closestMultiple + OFFSET);
						//System.out.println("("+odometer.getX()+", "+odometer.getY()+") +X");
						Sound.beep();
					}
				}
				else if (theta <= 270 + ANGLE_TOLERANCE && theta >= 270 - ANGLE_TOLERANCE && (Math.abs((X + OFFSET) % SQUARE_SIDE) <= DISTANCE_TOLERANCE || Math.abs((X + OFFSET + DISTANCE_TOLERANCE) % SQUARE_SIDE) <= DISTANCE_TOLERANCE)) { //traveling in -X
					closestMultiple = closestMultipleOfSquareSide(Y);
					thisCorrection = closestMultiple + 4;
					if(thisCorrection != lastCorrection) {
						//System.out.println("("+X+", "+Y+") -X");
						odometer.setY(closestMultiple - OFFSET);
						//System.out.println("("+odometer.getX()+", "+odometer.getY()+") -X");
						Sound.beep();
					}
				}
				else { //a line was detected but no odometer changes were made.
					Sound.buzz();
					//System.out.println("("+X+", "+Y+")");
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
	}
	
	//returns the closest multiple of square_side given a value within the distance  tolerance band
	private int closestMultipleOfSquareSide(double val) {
		return ((int) (val + DISTANCE_TOLERANCE) / SQUARE_SIDE) * SQUARE_SIDE;
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
		System.out.println(newVal + ", " + odometer.getX()+ ", " + odometer.getY());
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
		return result;
	}
	
}
