/**
 * ColorLocalisationData.java
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.Arrays;

/**
 * The ColorLocalisationData class is used to process the data coming from the color poller
 * @author Michael Vaquier
 *
 */

public class ColorLocalisationData {
	
	//Associations
	private Localisation localisation;
	
	private static final int SAMPLE_POINTS = 5;
	private int lastData = -1;
	
	private static final int DIFFERENCE_THRESHOLD = 4;
	private static final int DIFFERENCE_POINTS = 10;
	private int differenceCounter = DIFFERENCE_POINTS;
	
	private boolean lowPulse = false;
	

	
	public ColorLocalisationData(Localisation localisation) {
		this.localisation = localisation;
		
	}
	
	public void processData(int newVal) {
		int difference;
		if (lastData < 0) {
			lastData = newVal;
		} else {
			difference = (newVal - lastData);
			if (difference < DIFFERENCE_THRESHOLD) {
				lowPulse = true;
				differenceCounter = DIFFERENCE_POINTS;
			} else if (lowPulse && difference > DIFFERENCE_THRESHOLD) {
				localisation.resumeThread();
			} else {
				differenceCounter--;
				if(differenceCounter < 0) {
					lowPulse = false;
					differenceCounter = DIFFERENCE_POINTS;
				}
			}
		}
	}
}
