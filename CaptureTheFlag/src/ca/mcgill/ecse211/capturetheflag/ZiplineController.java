/**
 * ZiplineController.java
 */

package ca.mcgill.ecse211.capturetheflag;

import ca.mcgill.ecse211.capturetheflag.ColorPoller.ColorPollingState;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The Zipline Controller class holds the login in the zipline traversal sub task.
 * Once the motors are started, the thread waits until it receives an interrupt from the data processing class
 * to update the odometer values based on the game parameters and the game parameters and stops the motors shortly after.
 * @author Michael Vaquier
 *
 */

public class ZiplineController {
	
	//Lock objects for threading
	private Object pauseLock = new Object();
	private volatile boolean paused = false;
	
	private static final int DRIVE_SPEED = 200;
	private static final int PULLEY_SPEED = 200;
	
	private GameParameters gameParameters;
	
	//Associations
	private Odometer odometer;
	private ColorPoller colorPoller;
	
	//Motors
	private EV3LargeRegulatedMotor rightMotor;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor topMotor;
	
	/**
	 * Constructs an instance of the ZiplineController class.
	 * @param odometer  Association to Odometer instance
	 * @param colorPoller  Association to ColorPoller instance
	 * @param rightMotor  Reference to right motor
	 * @param leftMotor  Reference to the left motor
	 * @param topMotor  Reference to the top motor
	 * @param gameParameters  Game parameters for this round
	 */
	public ZiplineController(Odometer odometer, ColorPoller colorPoller, EV3LargeRegulatedMotor rightMotor, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor topMotor, GameParameters gameParameters) {
		this.odometer = odometer;
		this.colorPoller = colorPoller;
		
		this.rightMotor = rightMotor;
		this.leftMotor = leftMotor;
		this.topMotor = topMotor;
		
		this.gameParameters = gameParameters;
		
		colorPoller.getZiplineLightData().setZiplineController(this);
	}
	
	/**
	 * Rnns the routine to traverse the zipline.
	 * Turns the motors on, waits for an interrupt to indicate that the robot has landed,
	 * update the odometer values accordingly and stops the motors shortly after.
	 */
	public void runZiplineTask() {
		colorPoller.startPolling(ColorPollingState.ZIPLINING);
		rightMotor.setSpeed(DRIVE_SPEED);
		leftMotor.setSpeed(DRIVE_SPEED);
		topMotor.setSpeed(PULLEY_SPEED);
		rightMotor.forward();
		leftMotor.forward();
		topMotor.forward();
		pauseThread();
		colorPoller.stopPolling();
		
		//TODO Update the odometer depending on where the end of the zipline is.
	}

	
	/**
	 * Makes the thread calling the method pause itself.
	 */
	private void pauseThread() {
		paused = true;
		synchronized (pauseLock) {
            if (paused) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
	}
	
	/**
	 * Wakes up all threads waiting on this instance. 
	 */
    public void resumeThread() {
        synchronized (pauseLock) {
        	if (paused) {
        		paused = false;
        		pauseLock.notifyAll(); // Unblocks thread
        	}
        }
    }
    
}
