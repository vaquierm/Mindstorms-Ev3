package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

public class ColorPoller implements Runnable {
	
	private volatile boolean polling = false;
	
	private static final int POLLING_PERIOD = 10;
	
	private ColorPollingState state = ColorPollingState.LOCALISATION;
	private Object stateLock = new Object();
	
	//data processing classes
	private ColorLocalisationData colorLocalisationData;
	
	SampleProvider colorRedFront;
	float[] colorRedDataFront;
	SampleProvider colorRedBack;
	float[] colorRedDataBack;
	SampleProvider colorRedSide;
	float[] colorRedDataSide;
	
	public ColorPoller(ColorLocalisationData colorLocalisationData, SampleProvider colorRedBack, float[] colorRedDataBack, SampleProvider colorRedFront, float[] colorRedDataFront, SampleProvider colorRedSide, float[] colorRedDataSide) {
		this.colorRedFront = colorRedFront;
		this.colorRedDataFront = colorRedDataFront;
		this.colorRedBack = colorRedBack;
		this.colorRedDataBack = colorRedDataBack;
		this.colorRedSide = colorRedSide;
		this.colorRedDataSide = colorRedDataSide;
	}
	
	public void run() {
		long correctionStart, correctionEnd;
		polling = true;
		while (polling) {
			correctionStart = System.currentTimeMillis();

			switch(getPollingState()) {
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
		colorRedBack.fetchSample(colorRedDataBack, 0);
		colorLocalisationData.processData((int) (colorRedDataBack[0] * 100));
	}
	
	public void setPollingState(ColorPollingState state) {
		synchronized (stateLock) {
			this.state = state;
		}
	}
	
	public ColorPollingState getPollingState() {
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
	
	public enum ColorPollingState { LOCALISATION, ZIPLINING, BLOCK_SEARCHING }

}
