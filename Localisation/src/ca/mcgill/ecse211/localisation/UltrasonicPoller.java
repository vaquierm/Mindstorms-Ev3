package ca.mcgill.ecse211.localisation;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * Control of the wall follower is applied periodically by the UltrasonicPoller
 * thread. The while loop at the bottom executes in a loop. Assuming that the
 * us.fetchSample, and cont.processUSData methods operate in about 20mS, and
 * that the thread sleeps for 50 mS at the end of each loop, then one cycle
 * through the loop is approximately 70 mS. This corresponds to a sampling rate
 * of 1/70mS or about 14 Hz.
 *
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class UltrasonicPoller extends Thread {

	private Localisation localisation;
	
	private boolean polling = true;
	private Object lock = new Object();
	
	private SampleProvider us;
	private float[] usData;

	private int[] sensorData; // This array keeps the last SAMPLE_POINT points
								// of the us sensor
	private static final int SAMPLE_POINTS = 5;
	private int dataCounter = 0;
	private int lastAverage = -1;
	private int currentAverage = -1;


	private static final int EDGE_THRESHOLD = 40;

	private boolean fallingEdge = true;

	public UltrasonicPoller(SampleProvider us, float[] usData) {
		this.us = us;
		this.usData = usData;
		
		this.sensorData = new int[SAMPLE_POINTS];
		for (int i = 0; i < SAMPLE_POINTS; i ++) {
			this.sensorData[i] = -1;
		}
	}

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
				checkThreshold();
			}

			try {
				Thread.sleep(50);
			} catch (Exception e) {
			} // Poor man's timed sampling
		}
		return;
	}

	private void checkThreshold() {
		boolean edge = false;
		if (fallingEdge && currentAverage < EDGE_THRESHOLD && lastAverage > EDGE_THRESHOLD && lastAverage > 0) {
			edge = true;
		} else if (!fallingEdge && currentAverage > EDGE_THRESHOLD && lastAverage < EDGE_THRESHOLD && lastAverage > 0) {
			edge = true;
		}
		if(edge) {
			localisation.setWaiting(false);
			Sound.beep();
			try {
				this.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
	}

	private void newSensorValue(int newVal) {

		if (dataCounter < SAMPLE_POINTS) {
			sensorData[dataCounter] = newVal;
			currentAverage = newVal; // for the first few points the average is
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
		lastAverage = currentAverage;
		int newAverage = lastAverage + ((newVal - sensorData[0]) / SAMPLE_POINTS);
		// System.out.println("[" + sensorData[0] + ", " + sensorData[1] + ", "
		// + sensorData[2] + ", " + sensorData[3] + ", " + sensorData[4]+ "]");
		// System.out.println("new Average : "+ newAverage);
		currentAverage = newAverage;
	}

	// this method changes the detection mode of the robot
	public void setFallingEdgeMode(boolean b) {
		fallingEdge = b;
	}
	
	public void setLocalisation(Localisation loc) {
		this.localisation = loc;
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
