/**
 * ColorLocalisationData.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * The ColorLocalisationData class is used to process the data coming from the color poller
 * An interrupt is sent to the Localisation class when a line is detected.
 * @author Michael Vaquier
 *
 */

public class ColorLocalisationData {
	
	//Associations
	private Localisation localisation;
	
	private int lastData = -1;
	
	private static final int DIFFERENCE_THRESHOLD = 4;
	private static final int DIFFERENCE_POINTS = 10;
	private int differenceCounter = DIFFERENCE_POINTS;
	
	private boolean lowPulse = false;
	

	/**
	 * Creates a ColorLocalisationData object.
	 */
	public ColorLocalisationData() {
	}
	
	public void processData(int newVal) {
		int difference;
		if (lastData < 0) {
			lastData = newVal;
		} else {
			difference = (newVal - lastData);
			if (difference < -DIFFERENCE_THRESHOLD) {
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
	
	/**
	 * This method sets the association to an instance of the Localisation class.
	 * @param localisation
	 */
	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}
}
