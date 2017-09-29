package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class PController implements UltrasonicController {

	/* Constants */
	private static final int MOTOR_SPEED = 140;
	private static final int FILTER_OUT = 45;
	private final int ERROR_PROPORTIONALITY = 35;
	private final int CORNER_SPEED_DIFFERENCE = 45;
	private static final int SPIN_SPEED = 150;

	private final int bandCenter;
	private final int bandWidth;
	private int wheelSpeedDifference;
	private int distance;
	private int filterControl;
	private boolean spinning = false;
	private boolean corner = false;

	public PController(int bandCenter, int bandWidth) {
		this.bandCenter = bandCenter;
		this.bandWidth = bandWidth;
		this.filterControl = 0;

		LocalisationLab.leftMotor.setSpeed(MOTOR_SPEED); // Initalize motor
															// rolling forward
		LocalisationLab.rightMotor.setSpeed(MOTOR_SPEED);
		LocalisationLab.leftMotor.forward();
		LocalisationLab.rightMotor.forward();
	}

	@Override
	public void processUSData(int distance) {

		// rudimentary filter - toss out invalid samples corresponding to null
		// signal.
		// (n.b. this was not included in the Bang-bang controller, but easily
		// could have).
		//
		if (distance >= 100 && filterControl < FILTER_OUT) {
			// bad value increment the filter value
			// set distance to bandCenter to go straight
			filterControl++;
			this.distance = bandCenter;
		} else if (distance >= 100) {
			// We have repeated large values, so there must actually be nothing
			this.distance = distance;
		} else {
			// distance went below 100: reset filter
			filterControl = 0;
			this.distance = distance;
		}

		corner = false;
		if (this.distance >= 100) {
			wheelSpeedDifference = CORNER_SPEED_DIFFERENCE;
			LocalisationLab.leftMotor.setSpeed(140);
			LocalisationLab.rightMotor.setSpeed(240);
			LocalisationLab.rightMotor.forward();
			LocalisationLab.leftMotor.forward();
			corner = true;
		} else {
			wheelSpeedDifference = Math.abs(ERROR_PROPORTIONALITY * (bandCenter - distance) / 10);
			if (wheelSpeedDifference > 90)
				wheelSpeedDifference = 90;
		}

		if (!corner) {
			if (Math.abs(this.distance - bandCenter) < bandWidth) { // Sweet
																	// spot
				LocalisationLab.leftMotor.setSpeed(MOTOR_SPEED);
				LocalisationLab.rightMotor.setSpeed(MOTOR_SPEED);
				LocalisationLab.rightMotor.forward();
				LocalisationLab.leftMotor.forward();
			} else if (this.distance < bandCenter) { // Too close
				if (this.distance < 10) {
					LocalisationLab.leftMotor.setSpeed(300);
					LocalisationLab.rightMotor.setSpeed(300);
					LocalisationLab.rightMotor.backward();
					LocalisationLab.leftMotor.backward();
					spinning = false;
				} else if (this.distance < 20) { // if the robot is way too
													// close, it spins
					LocalisationLab.leftMotor.setSpeed(SPIN_SPEED);
					LocalisationLab.rightMotor.setSpeed(SPIN_SPEED);
					LocalisationLab.rightMotor.backward();
					LocalisationLab.leftMotor.forward();
					spinning = true;
				} else {
					LocalisationLab.leftMotor.setSpeed(MOTOR_SPEED + wheelSpeedDifference);
					LocalisationLab.rightMotor.setSpeed(MOTOR_SPEED - wheelSpeedDifference);
					LocalisationLab.rightMotor.forward();
					LocalisationLab.leftMotor.forward();
					spinning = false;
				}
			} else { // Too far
				LocalisationLab.leftMotor.setSpeed(MOTOR_SPEED - wheelSpeedDifference);
				LocalisationLab.rightMotor.setSpeed(MOTOR_SPEED + wheelSpeedDifference);
				LocalisationLab.rightMotor.forward();
				LocalisationLab.leftMotor.forward();
				spinning = false;
			}
		}

	}

	@Override
	public int readUSDistance() {
		return this.distance;
	}

}
