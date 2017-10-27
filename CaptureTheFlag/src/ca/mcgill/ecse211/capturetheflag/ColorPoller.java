package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

public class ColorPoller implements Runnable {
	
	private boolean polling = false;
	private Object pollingLock = new Object();
	
	private static final int POLLING_PERIOD = 10;
	
	private ColorPollingState state = ColorPollingState.LOCALISATION;
	
	SampleProvider colorRedFront;
	float[] colorRedDataFront;
	SampleProvider colorRedBack;
	float[] colorRedDataBack;
	SampleProvider colorRedSide;
	float[] colorRedDataSide;
	
	public ColorPoller(SampleProvider colorRedBack, float[] colorRedDataBack, SampleProvider colorRedFront, float[] colorRedDataFront, SampleProvider colorRedSide, float[] colorRedDataSide) {
		this.colorRedFront = colorRedFront;
		this.colorRedDataFront = colorRedDataFront;
		this.colorRedBack = colorRedBack;
		this.colorRedDataBack = colorRedDataBack;
		this.colorRedSide = colorRedSide;
		this.colorRedDataSide = colorRedDataSide;
	}
	
	public void run() {
		long correctionStart, correctionEnd;
		synchronized (pollingLock) {
			polling = true;
		}
		while (getPolling()) {
			correctionStart = System.currentTimeMillis();

			switch(state) {
			case LOCALISATION:
				processLocalisation();
				break;
			case ZIPLINING:
				processZiplining();
				break;
			case BLOCK_SEARCHING:
				processBlockSearching();
				break;
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
	
	private void processBlockSearching() {
		// TODO 
		//colorRed.fetchSample(colorRedData, 0);
		//int sample = (int) (colorRedData[0] * 100);
		
	}

	private void processZiplining() {
		// TODO Auto-generated method stub
		
	}

	private void processLocalisation() {
		// TODO Auto-generated method stub
		
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
	
	public enum ColorPollingState { LOCALISATION, ZIPLINING, BLOCK_SEARCHING }

}
