/**
 * UltrasonicPoller.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

/**
 * The UltrasonicPoller class has a reference of one ultrasonic poller and polls data
 * periodically once its thread is started and stops when it is indicated to by terminating itself.
 * The data fetched from the sensor is then sent to the correct data processing class according to
 * the current polling state.
 * @author Michael Vaquier
 *
 */

public class UltrasonicPoller implements Runnable {
	
	private volatile boolean polling = false;
	
	private static final int POLLING_PERIOD = 40;
	
	//data processing
	private UltrasonicLocalisationData ultrasonicLocalisationData;
	private UltrasonicNavigationData ultrasonicNavigationData;
	
	private UltrasonicPollingState state = UltrasonicPollingState.LOCALISATION;
	private Object stateLock = new Object();
	
	SampleProvider usDistance;
	float[] usData;
	
	/**
	 * Creates an instance of the UltrasonicPoller class.
	 * @param usDistance  Ultrasonic sensor
	 * @param usData  Ultrasonic sensor fetching array
	 * @param ultrasonicLocalisationData  Association to UltrasonicLocalisationData instance
	 * @param ultrasonicNavigationData  Association to UltrasonicNavigationData instance
	 */
	public UltrasonicPoller(SampleProvider usDistance, float[] usData, UltrasonicLocalisationData ultrasonicLocalisationData, UltrasonicNavigationData ultrasonicNavigationData) {
		this.usDistance = usDistance;
		this.usData = usData;
		this.ultrasonicLocalisationData = ultrasonicLocalisationData;
		this.ultrasonicNavigationData = ultrasonicNavigationData;
	}
	
	/**
	 * Called when a new thread is spawned with this poller.
	 * Starts polling the ultrasonic sensor periodically.
	 */
	public void run() {
		long correctionStart, correctionEnd;
		polling = true;
		long startTime = System.currentTimeMillis();
		while (polling) {
			correctionStart = System.currentTimeMillis();
			usDistance.fetchSample(usData, 0);
			int sample = (int) (usData[0] * 100);
			if(sample > 0) {
				switch(getPollingState()) {
				case LOCALISATION:
					if(sample > 255)
						sample = 255;
					System.out.println((correctionStart - startTime) + ", " +sample);
					ultrasonicLocalisationData.processData(sample);
					break;
				case NAVIGATION:
					ultrasonicNavigationData.processData(sample);
					break;
				}
			}
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < POLLING_PERIOD) {
				try {
					Thread.sleep(POLLING_PERIOD - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	/**
	 * Sets the polling state of the poller.
	 * @param state  New polling state to be set.
	 */
	public void setPollingState(UltrasonicPollingState state) {
		synchronized (stateLock) {
			this.state = state;
		}
	}
	
	/**
	 * Returns the current polling state of the poller.
	 * @return  Current polling state
	 */
	public UltrasonicPollingState getPollingState() {
		synchronized (stateLock) {
			return state;
		}
	}
	
	/**
	 * Gets the reference to the UltrasonicLocalisationData association.
	 * @return  Reference to UtrasonicLocalisationData instance
	 */
	public UltrasonicLocalisationData getUltrasonicLocalisationData() {
		return ultrasonicLocalisationData;
	}
	
	/**
	 * Gets the reference to the UltrasonicNaviagtionData association.
	 * @return  Reference to UltrasonicNavigationData instance
	 */
	public UltrasonicNavigationData getUltrasonicNavigationData() {
		return ultrasonicNavigationData;
	}
	
	/**
	 * Spawns a thread that starts polling and processes data accordingly to its polling state.
	 * @param newPollingState  The state in which the poller will opperate.
	 */
	public void startPolling(UltrasonicPollingState newPollingState) {
		setPollingState(newPollingState);
		new Thread(this).start();
	}
	
	
	/**
	 * Stop the polling process by letting the thread terminate itself.
	 */
	public void stopPolling() {
		polling = false;
	}
	
	/**
	 * This enumeration indicates the different states in which the UltrasonicPoller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum UltrasonicPollingState { LOCALISATION, NAVIGATION }

}
