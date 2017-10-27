package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

public class UltrasonicPoller implements Runnable {
	
	private boolean polling = false;
	private Object pollingLock = new Object();
	
	private static final int POLLING_PERIOD = 10;
	
	private UltrasonicPollingState state = UltrasonicPollingState.LOCALISATION;
	
	SampleProvider usDistance;
	float[] usData;
	
	public UltrasonicPoller(SampleProvider usDistance, float[] usData) {
		this.usDistance = usDistance;
		this.usData = usData;
	}
	
	public void run() {
		long correctionStart, correctionEnd;
		synchronized (pollingLock) {
			polling = true;
		}
		while (getPolling()) {
			correctionStart = System.currentTimeMillis();

			usDistance.fetchSample(usData, 0);
			int sample = (int) (usData[0] * 100);
			
			if(sample > 0) {
				switch(state) {
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
	
	private boolean getPolling() {
		synchronized(pollingLock) {
			return polling;
		}
	}
	
	public void stopPolling() {
		synchronized(pollingLock) {
			polling = false;
		}
	}
	
	public enum UltrasonicPollingState { LOCALISATION, NAVIGATION }

}
