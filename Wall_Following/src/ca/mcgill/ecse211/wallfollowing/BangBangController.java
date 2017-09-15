package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.*;

public class BangBangController implements UltrasonicController {
	
	  private static final int FILTER_OUT = 40;
	  private static final int SPIN_SPEED = 190;

  private final int bandCenter;
  private final int bandwidth;
  private final int motorLow;
  private final int motorHigh;
  private int distance;
  
  private int filterControl;
  private boolean spinning = false;

  public BangBangController(int bandCenter, int bandwidth, int motorLow, int motorHigh) {
    // Default Constructor
    this.bandCenter = bandCenter;
    this.bandwidth = bandwidth;
    this.motorLow = motorLow;
    this.motorHigh = motorHigh;
    WallFollowingLab.leftMotor.setSpeed(motorLow); // Start robot moving forward
    WallFollowingLab.rightMotor.setSpeed(motorLow);
    WallFollowingLab.leftMotor.forward();
    WallFollowingLab.rightMotor.forward();
  }

  @Override
  public void processUSData(int distance) {
	  
	if (distance >= 255 && filterControl < FILTER_OUT) {
      // bad value, do not set the distance var, however do increment the
      // filter value
      filterControl++;
    }
	 else if (distance >= 255) {
      // We have repeated large values, so there must actually be nothing
      // there: leave the distance alone
      this.distance = distance;
    } else {
      // distance went below 255: reset filter and leave
      // distance alone.
      filterControl = 0;
      this.distance = distance;
    }
	
	
    if (Math.abs(this.distance - bandCenter) < bandwidth) { //Sweet spot
    	WallFollowingLab.leftMotor.setSpeed(motorLow);
        WallFollowingLab.rightMotor.setSpeed(motorLow);
        WallFollowingLab.rightMotor.forward();
        spinning = false;
    }
    else if (this.distance < bandCenter) { //Too close
    	if (this.distance < 20) { // if the robot is way too close, it spins
    		WallFollowingLab.leftMotor.setSpeed(SPIN_SPEED);
    		WallFollowingLab.rightMotor.setSpeed(SPIN_SPEED);
    	    WallFollowingLab.rightMotor.backward();
    	    spinning = true;
    	}
    	else {
    		WallFollowingLab.leftMotor.setSpeed(motorHigh);
    		WallFollowingLab.rightMotor.setSpeed(motorLow);
    		WallFollowingLab.rightMotor.forward();
    		spinning = false;
    	}
    }
    else { //Too far
        WallFollowingLab.leftMotor.setSpeed(motorLow);
        WallFollowingLab.rightMotor.setSpeed(motorHigh);
        WallFollowingLab.rightMotor.forward();
        spinning = false;
    }
  }

  @Override
  public int readUSDistance() {
    return this.distance;
  }
}
