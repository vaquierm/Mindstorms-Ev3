/**
 * ZiplineController.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * The ZipllineController is used to traverse the zipline. When running it, it is assumed that the robot is perfectly aligned with the zipline
 * and that all it has to do is to roll forward to mount it.
 * The association to the color poller is able to get data from the downward facing sensor to be able to know when the robot is off the ground.
 * After starting the wheels, the thread running here will be idle and will wait from an interrupt to indicate that the robot has "landed"
 * After landing, the Odometer is updated accordingly by looking at the values from the GameParameters.
 * @author Michael Vaquier
 *
 */

public class ZiplineLightData {
	
	//associations
	private ZiplineController ziplineController;
	
	private int lastData = -1;
	
	private static final int DIFFERENCE_THRESHOLD = 4;
	private static final int DIFFERENCE_POINTS = 10;
	private int differenceCounter = DIFFERENCE_POINTS;
	
	/**
	 * Constructs an instance of the ZiplineLightData class.
	 */
	public ZiplineLightData() {
	}
	
	/**
	 * Takes as an input data coming from the color poller and processes it to detect is the robot has landed.
	 * When the detection is made, an interrupt is sent to the ziplineController instance it is associated to to indicate this event.
	 * @param newVal  New value fetched from the color sensor
	 */
	public void processData(int newVal) {
		System.out.println(newVal);
		int difference;
		if (lastData < 0) {
			lastData = newVal;
		} else {
			difference = (newVal - lastData);
			if (difference > DIFFERENCE_THRESHOLD) {
				ziplineController.resumeThread();
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	/**
	 * Sets the ZiplineController association to this instance.
	 * @param ziplineController  Association to ZiplineController instance
	 */
	public void setZiplineController(ZiplineController ziplineController) {
		this.ziplineController = ziplineController;
	}
	
}
