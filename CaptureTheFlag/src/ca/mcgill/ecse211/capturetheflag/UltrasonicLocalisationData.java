/**
 * UltrasonicLocalisationData.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * The UltrasonoicLocationData class is used to process date from the
 * ultrasonic poller to detect edges
 * 
 * @author Michael Vaquier
 *
 */

public class UltrasonicLocalisationData {
	
	private Localisation localisation;
	
	private boolean fallingEdge = false;
	private boolean foundFirstEdge = false;
	
	private int lastData = -1;
	
	private static final int EDGE_THRESHOLD = 50;
	
	public UltrasonicLocalisationData(Localisation localisation) {
		this.localisation = localisation;
	}
	
	public void processData(int newVal) {
		if (lastData < 0) {
			lastData = newVal;
		} else {
			if (!foundFirstEdge) {
				if (lastData > EDGE_THRESHOLD && newVal < EDGE_THRESHOLD) {
					fallingEdge = true;
					foundFirstEdge = true;
					localisation.resumeThread();
					threadWait();
				} else if (lastData < EDGE_THRESHOLD && newVal > EDGE_THRESHOLD) {
					fallingEdge = false;
					foundFirstEdge = true;
					localisation.resumeThread();
					threadWait();
				}
			} else {
				if (fallingEdge && lastData > EDGE_THRESHOLD && newVal < EDGE_THRESHOLD) {
					localisation.resumeThread();
					foundFirstEdge = false;
					threadWait();
				} else if (!fallingEdge && lastData < EDGE_THRESHOLD && newVal > EDGE_THRESHOLD) {
					localisation.resumeThread();
					foundFirstEdge = false;
					threadWait();
				}
			}
		}
	}
	
	private void threadWait() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

}
