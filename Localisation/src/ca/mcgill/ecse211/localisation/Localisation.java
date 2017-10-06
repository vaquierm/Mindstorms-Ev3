package ca.mcgill.ecse211.localisation;

public class Localisation {
	
	private UltrasonicPoller usPoller;
	private ColorPoller colorPoller;
	private static final int ROTATION_SPEED = 150;
	
	private static final int COLOR_SENSOR_OFFSET = 15;
	
	private boolean fallingEdge;
	
	private boolean waiting = false;
	private Object lock = new Object();
	
	private double[] edges = {-1, -1};
	private double[] lines = {-1, -1, -1, -1};
	
	public Localisation(UltrasonicPoller usPoller, ColorPoller colorPoller, boolean fallingEdge) {
		this.usPoller = usPoller;
		this.fallingEdge = fallingEdge;
		this.colorPoller = colorPoller;
	}
	
	public void alignAngle() {
		usPoller.start();
		LocalisationLab.leftMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.rightMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.leftMotor.backward();
		LocalisationLab.rightMotor.forward();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		edges[0] = LocalisationLab.odometer.getThetaDegrees();
		LocalisationLab.leftMotor.forward();
		LocalisationLab.rightMotor.backward();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		edges[1] = LocalisationLab.odometer.getThetaDegrees();
		LocalisationLab.leftMotor.stop(true);
		LocalisationLab.rightMotor.stop();
		LocalisationLab.odometer.setTheta(Math.toRadians(computeAngle()));
		usPoller.stopPolling();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}
	
	public void fixXY() {
		
		colorPoller.start();
		LocalisationLab.leftMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.rightMotor.setSpeed(ROTATION_SPEED);
		LocalisationLab.leftMotor.backward();
		LocalisationLab.rightMotor.forward();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		lines[0] = LocalisationLab.odometer.getThetaDegrees();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		lines[2] = LocalisationLab.odometer.getThetaDegrees();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		lines[1] = LocalisationLab.odometer.getThetaDegrees();
		setWaiting(true);
		while(getWaiting()) {
			
		}
		lines[3] = LocalisationLab.odometer.getThetaDegrees();
		LocalisationLab.leftMotor.stop(true);
		LocalisationLab.rightMotor.stop();
		
		colorPoller.stopPolling();
		LocalisationLab.odometer.setX(computeX());
		LocalisationLab.odometer.setY(computeY());
	}
	
	private double computeX() {
		double thetaD = (lines[0] - lines[1])/2;
		if(thetaD < 0) {
			thetaD += 360;
		}
		return -COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD));
	}
	
	private double computeY() {
		double thetaD = (lines[2] - lines[3])/2;
		if(thetaD < 0) {
			thetaD += 360;
		}
		return -COLOR_SENSOR_OFFSET * Math.cos(Math.toRadians(thetaD));
	}
	
	private double computeAngle() {
		double heading = edges[1] - edges[0];
		if (heading < 0) {
			heading += 360;
		}
		heading /= 2;
		if (!fallingEdge) {
			heading = 225 + heading;
		}
		else {
			heading = 45 + heading;
		}
		return heading;
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
