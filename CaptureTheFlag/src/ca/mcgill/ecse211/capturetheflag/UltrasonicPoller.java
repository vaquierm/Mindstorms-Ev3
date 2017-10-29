package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

public class UltrasonicPoller implements Runnable {
	
	private volatile boolean polling = false;
	
	private static final int POLLING_PERIOD = 10;
	
	private UltrasonicPollingState state = UltrasonicPollingState.LOCALISATION;
	private Object stateLock = new Object();
	
	SampleProvider usDistance;
	float[] usData;
	
	public UltrasonicPoller(SampleProvider usDistance, float[] usData) {
		this.usDistance = usDistance;
		this.usData = usData;
	}
	
	public void run() {
		long correctionStart, correctionEnd;
		polling = true;

		while (polling) {
			correctionStart = System.currentTimeMillis();

			usDistance.fetchSample(usData, 0);
			int sample = (int) (usData[0] * 100);
			
			if(sample > 0) {
				switch(getPollingState()) {
				case LOCALISATION:
					//TODO
					break;
				case NAVIGATION:
					//TODO
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
	
	public void setPollingState(UltrasonicPollingState state) {
		synchronized (stateLock) {
			this.state = state;
		}
	}
	
	public UltrasonicPollingState getPollingState() {
		synchronized (stateLock) {
			return state;
		}
	}
	
	public void startPolling() {
		new Thread(this).start();
	}
	
	
	public void stopPolling() {
		polling = false;
	}
	
	public enum UltrasonicPollingState { LOCALISATION, NAVIGATION }

}
