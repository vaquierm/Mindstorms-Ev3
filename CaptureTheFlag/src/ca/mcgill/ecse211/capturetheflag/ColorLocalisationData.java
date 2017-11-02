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
	
	/**
	 * This method is used to process data coming from the color poller
	 * to determine if the color sensor is crossing a line.
	 * When a line is detected, an interrupt is sent to the localisation association to alert
	 * if of this event.
	 * @param newVal  New value read by the sensor.
	 */
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
	 * @param localisation  The localisation instance that will be set as an association.
	 */
	public void setLocalisation(Localisation localisation) {
		this.localisation = localisation;
	}
}
