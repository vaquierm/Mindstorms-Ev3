package ca.mcgill.ecse211.zipline;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The zipline controller is a thread that completes the entire zipline procedure
 * @author Michael Vaquier
 *
 */

public class ZiplineController extends Thread {
	
	private static final int ZIPLINE_MOUNT_SPEED = 200;
	private static final int ZIPLINE_ARM_SPEED = 200;
	
	private boolean waiting = false;
	
	private Object lock = new Object();
	
	public static final EV3LargeRegulatedMotor armMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	
	public ZiplineController() {
		
	}
	
	public void run() {
		setWaiting(false);
		ZipLineLab.leftMotor.setSpeed(ZIPLINE_MOUNT_SPEED);
		ZipLineLab.rightMotor.setSpeed(ZIPLINE_MOUNT_SPEED);
		armMotor.setSpeed(ZIPLINE_ARM_SPEED);
		ZipLineLab.leftMotor.forward();
		ZipLineLab.rightMotor.forward();
		armMotor.forward();
		setWaiting(true);
		while(getWaiting());
		ZipLineLab.leftMotor.stop(false);
		ZipLineLab.rightMotor.stop();
		setWaiting(true);
		while(getWaiting());
		armMotor.stop(false);
		ZipLineLab.leftMotor.rotate(1000, false);
		ZipLineLab.rightMotor.rotate(1000);
		
		return;
	}
	
	
	public boolean getWaiting() {
		synchronized(lock) {
			return waiting;
		}
	}
	
	public void setWaiting(boolean b) {
		synchronized(lock) {
			waiting = b;
		}
	}

}
