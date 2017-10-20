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
	
	ZiplineUSPoller ziplineUSPoller;
	
	public static final EV3LargeRegulatedMotor armMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
	public ZiplineController(ZiplineUSPoller ziplineUSPoller) {
		this.ziplineUSPoller = ziplineUSPoller;
		ziplineUSPoller.setController(this);
	}
	
	public void run() {
		ziplineUSPoller.start();
		setWaiting(false);
		ZipLineLab.leftMotor.setSpeed(ZIPLINE_MOUNT_SPEED);
		ZipLineLab.rightMotor.setSpeed(ZIPLINE_MOUNT_SPEED);
		armMotor.setSpeed(ZIPLINE_ARM_SPEED);
		ZipLineLab.leftMotor.forward();
		ZipLineLab.rightMotor.forward();
		armMotor.forward();
		setWaiting(true);
		while(getWaiting());
		armMotor.stop(true);
		ZipLineLab.leftMotor.stop(true);
		ZipLineLab.rightMotor.stop(true);
		ziplineUSPoller.stopPolling();
		
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
