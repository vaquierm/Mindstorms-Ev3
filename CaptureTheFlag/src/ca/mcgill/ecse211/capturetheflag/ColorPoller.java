/**
 * ColorPoller.java
 */

package ca.mcgill.ecse211.capturetheflag;

import lejos.robotics.SampleProvider;
import lejos.utility.Timer;
import lejos.utility.TimerListener;

/**
 * The ColorPoller class is used to periodically poll one or multiple color sensors
 * and send the data to be processed to a Data class
 * A state machine is incorporated such that the color sensor polled will be the correct one(s) and the data will be sent to the correct processing class.
 * @author Michael Vaquier
 *
 */

public class ColorPoller implements Runnable, TimerListener {
	
	private ColorPollingState state = ColorPollingState.LOCALISATION;
	private Object stateLock = new Object();
	
	//data processing classes
	private ColorLocalisationData colorLocalisationData;
	private ZiplineLightData ziplineLightData;
	private BlockSearchingData blockSearchingData;
	
	//Pollers
	private SampleProvider colorRedFront;
	private float[] colorRedDataFront;
	private SampleProvider colorRedBack;
	private float[] colorRedDataBack;
	private SampleProvider colorRedSide;
	private float[] colorRedDataSide;
	
	//timer and synchronization
	private Timer timer = null;
	private volatile boolean polling = false;
	private static final int POLLING_PERIOD = 10;
	private boolean timerMode;
	private boolean running = false;
	
	/**
	 * Creates a CollorPoller object.
	 * @param colorLocalisationData  Data processing instance for color localisation
	 * @param ziplineLightData  Data processing instance for zipline landing
	 * @param blockSearchingData  Data processing instance for confirming the color of the block
	 * @param colorRedBack  Back color sensor
	 * @param colorRedDataBack  Back color sensor data fetching array
	 * @param colorRedFront  Front color sensor
	 * @param colorRedDataFront  Front color sensor data fetching array
	 * @param colorRedSide  Side color sensor data
	 * @param colorRedDataSide  Side color sensor data fetching array
	 */
	public ColorPoller(SampleProvider colorRedBack, float[] colorRedDataBack, SampleProvider colorRedFront, float[] colorRedDataFront, SampleProvider colorRedSide, float[] colorRedDataSide,
			ColorLocalisationData colorLocalisationData, ZiplineLightData ziplineLightData, BlockSearchingData blockSearchingData) {
		this.colorRedFront = colorRedFront;
		this.colorRedDataFront = colorRedDataFront;
		this.colorRedBack = colorRedBack;
		this.colorRedDataBack = colorRedDataBack;
		this.colorRedSide = colorRedSide;
		this.colorRedDataSide = colorRedDataSide;
		
		this.colorLocalisationData = colorLocalisationData;
		this.ziplineLightData = ziplineLightData;
		this.blockSearchingData = blockSearchingData;
	}
	
	/**
	 * Method called when a thread is started with this runnable.
	 */
	public void run() {
		long correctionStart, correctionEnd;
		polling = true;
		while (polling) {
			correctionStart = System.currentTimeMillis();
			colorPollerProcess();
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
	 * Interrupt service routine called when the poller is running in timer mode.
	 */
	public void timedOut() {
		colorPollerProcess();
	}
	
	/**
	 * holds the logic of one iteration of the color poller.
	 */
	private void colorPollerProcess() {
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
	}
	
	/**
	 * Polls the front Color sensor and send the data to the blockColorData association to be processed
	 */
	private void processBlockSearching() {
		colorRedFront.fetchSample(colorRedDataFront, 0);
		blockSearchingData.processData((int) (colorRedDataFront[0]));
	}

	/**
	 * Polls the back Color sensor and send the data to the ziplineLighData association to be processed
	 */
	private void processZiplining() {
		colorRedBack.fetchSample(colorRedDataBack, 0);
		ziplineLightData.processData((int) (colorRedDataBack[0] * 100));
		
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
	 * @param state  New state to set the poller to
	 */
	public void setPollingState(ColorPollingState state) {
		synchronized (stateLock) {
			this.state = state;
		}
	}
	
	/**
	 * This method returns the association to the ColorLocalisationData instance
	 * @return The ColorLocalisationData association
	 */
	public ColorLocalisationData getColorLocalisationData() {
		return colorLocalisationData;
	}
	
	/**
	 * This method returns the association to the ZiplineLightData instance
	 * @return  The ZiplineLightData association
	 */
	public ZiplineLightData getZiplineLightData() {
		return ziplineLightData;
	}
	
	/**
	 * This method returns the association to the BlockSearchingData instance
	 * @return  The BlockSearchingData association
	 */
	public BlockSearchingData getBlockSearchingData() {
		return blockSearchingData;
	}
	
	/**
	 * Returns the polling state of the poller
	 * @return  Current polling state
	 */
	public ColorPollingState getPollingState() {
		synchronized (stateLock) {
			return state;
		}
	}
	
	/**
	 * This method can be will spawn a new thread and start the polling
	 */
	public void startPolling(ColorPollingState state) {
		if (!running) {
			setPollingState(state);
			if(state == ColorPollingState.LOCALISATION)
				colorLocalisationData.resetLastData();
			new Thread(this).start();
			timerMode = false;
			running = true;
		} else if (running && timerMode) {
			stopPolling();
			startPolling(state);
		}
	}
	
	/**
	 * This method creates a timer and starts the polling process of the light sensor.
	 * @param state 
	 */
	public void startPollingTimer(ColorPollingState state) {
		if(!running) {
			if(timer == null) {
				timer = new Timer(POLLING_PERIOD, this);
			}
			setPollingState(state);
			if(state == ColorPollingState.LOCALISATION)
				colorLocalisationData.resetLastData();
			timer.start();
			timerMode = true;
			running = true;
		} else if (running && !timerMode) {
			stopPolling();
			startPollingTimer(state);
		}
	}
	
	/**
	 * This method will stop the polling of the poller thread and will let it terminate itself.
	 */
	public void stopPolling() {
		if(running) {
			if(timerMode) {
				timer.stop();
			} else {
				polling = false;
			}
			running = false;
		}
	}
	
	
	/**
	 * This enumeration defines the states in which the ColorPoller can be in.
	 * @author Michael Vaquier
	 *
	 */
	public enum ColorPollingState { LOCALISATION, ZIPLINING, BLOCK_SEARCHING }

}
