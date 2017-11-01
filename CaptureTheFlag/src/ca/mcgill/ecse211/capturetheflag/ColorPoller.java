/**
 * ColorPoller.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;

/**
 * The ColorPoller class is used to periodically poll one or multiple color sensors
 * and send the data to be processed to a Data class
 * A state machine is incorporated such that the color sensor polled will be the correct one(s) and the data will be sent to the correct processing class.
 * @author Michael Vaquier
 *
 */

public class ColorPoller implements Runnable {
	
	private volatile boolean polling = false;
	
	private static final int POLLING_PERIOD = 10;
	
	private ColorPollingState state = ColorPollingState.LOCALISATION;
	private Object stateLock = new Object();
	
	//data processing classes
	private ColorLocalisationData colorLocalisationData;
	private ZiplineLightData ziplineLightData;
	
	SampleProvider colorRedFront;
	float[] colorRedDataFront;
	SampleProvider colorRedBack;
	float[] colorRedDataBack;
	SampleProvider colorRedSide;
	float[] colorRedDataSide;
	
	/**
	 * Creates a CollorPoller object.
	 * @param colorLocalisationData
	 * @param colorRedBack
	 * @param colorRedDataBack
	 * @param colorRedFront
	 * @param colorRedDataFront
	 * @param colorRedSide
	 * @param colorRedDataSide
	 */
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
	
	/**
	 * Polls the front Color sensor and send the data to the blockColorData association to be processed
	 */
	private void processBlockSearching() {
		// TODO 
		//colorRed.fetchSample(colorRedData, 0);
		//int sample = (int) (colorRedData[0] * 100);
		
	}

	/**
	 * Polls the back Color sensor and send the data to the ziplineLighData association to be processed
	 */
	private void processZiplining() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Polls the back Color sensor and send the data to the colorLocalisationData association to be processed
	 */
	private void processLocalisation() {
		colorRedBack.fetchSample(colorRedDataBack, 0);
		colorLocalisationData.processData((int) (colorRedDataBack[0] * 100));
	}
	
	/**
	 * Sets the polling state of the poller
	 * @param state
	 */
	public void setPollingState(ColorPollingState state) {
		synchronized (stateLock) {
			this.state = state;
		}
	}
	
	/**
	 * This method returns the association to the ColorLocalisationData instance
	 * @return
	 */
	public ColorLocalisationData getColorLocalisationData() {
		return colorLocalisationData;
	}
	
	/**
	 * This method returns the association to the ZiplineLightData instance
	 * @return
	 */
	public ZiplineLightData getZiplineLightData() {
		return ziplineLightData;
	}
	
	/**
	 * Returns the polling state of the poller
	 * @return
	 */
	public ColorPollingState getPollingState() {
		synchronized (stateLock) {
			return state;
		}
	}
	
	/**
	 * This method can be will spawn a new thread and start the polling
	 */
	public void startPolling() {
		new Thread(this).start();
	}
	
	/**
	 * This method will stop the polling of the poller thread and will let it terminate itself.
	 */
	public void stopPolling() {
		polling = false;
	}
	
	/**
	 * This enumeration defines the states in which the ColorPoller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum ColorPollingState { LOCALISATION, ZIPLINING, BLOCK_SEARCHING }

}
