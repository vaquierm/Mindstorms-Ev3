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
	private static final int PULLEY__FAST_SPEED = 300;
	private static final int PULLEY_FAST_ACCEL = 3000;
	private static final int PULLEY_SLOW_ACCEL = 200;
	
	
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
		topMotor.setAcceleration(PULLEY_FAST_ACCEL);
		rightMotor.forward();
		leftMotor.forward();
		topMotor.forward();
		pauseThread();
		rightMotor.stop(true);
		leftMotor.stop(true);
		topMotor.setAcceleration(PULLEY_SLOW_ACCEL);
		topMotor.setSpeed(PULLEY__FAST_SPEED);
		pauseThread();
		topMotor.setSpeed(PULLEY_SPEED);
		topMotor.stop(true);
		colorPoller.stopPolling();
		topMotor.rotate(300, false);
		double newTheta = Math.toDegrees(Math.atan2(gameParameters.ZO_R.x - gameParameters.ZC_R.x, gameParameters.ZO_R.y - gameParameters.ZC_R.y));
		
		if (newTheta < 0)
			newTheta += 360;
		newTheta = Math.round(newTheta);
		if ( newTheta % 45 == 0) {
			
			switch ((int) newTheta) {
			case 45:
				odometer.setX(gameParameters.ZC_R.x - 10);
				odometer.setY(gameParameters.ZC_R.y - 10);
				break;
			case 135:
				odometer.setX(gameParameters.ZC_R.x - 10);
				odometer.setY(gameParameters.ZC_R.y + 10);
				break;
			case 225:
				odometer.setX(gameParameters.ZC_R.x + 10);
				odometer.setY(gameParameters.ZC_R.y + 10);
				break;
			case 315:
				odometer.setX(gameParameters.ZC_R.x + 10);
				odometer.setY(gameParameters.ZC_R.y - 10);
				break;
			default:
				break;
			}
		} else {
			odometer.setX(gameParameters.ZC_R.x);
			odometer.setY(gameParameters.ZC_R.y);
		}
		odometer.setTheta(Math.toRadians(newTheta));
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
