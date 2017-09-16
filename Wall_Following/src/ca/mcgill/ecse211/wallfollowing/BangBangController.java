package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.*;

public class BangBangController implements UltrasonicController {
	
	  private static final int FILTER_OUT = 35;
	  private static final int SPIN_SPEED = 175;

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
	  
	if (distance >= 100 && filterControl < FILTER_OUT) {
      // bad value, do not set the distance var, however do increment the
      // filter value
      filterControl++;
      this.distance = bandCenter; //while waiting for filter control to confirm that there is indeed nothing there, the robot goes straight
    }
	 else if (distance >= 100) {
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
        WallFollowingLab.leftMotor.forward();
        spinning = false;
    }
    else if (this.distance < bandCenter) { //Too close
    	if (this.distance < 10) { // if the robot is way way too close it backs up fast
    		WallFollowingLab.leftMotor.setSpeed(300);
    		WallFollowingLab.rightMotor.setSpeed(300);
    	    WallFollowingLab.rightMotor.backward();
    	    WallFollowingLab.leftMotor.backward();
    	    spinning = true;
    	}
    	else if (this.distance < 20) { // if the robot is way too close, it spins
    		WallFollowingLab.leftMotor.setSpeed(SPIN_SPEED);
    		WallFollowingLab.rightMotor.setSpeed(SPIN_SPEED);
    	    WallFollowingLab.rightMotor.backward();
    	    WallFollowingLab.leftMotor.forward();
    	    spinning = true;
    	}
    	else {
    		WallFollowingLab.leftMotor.setSpeed(motorHigh);
    		WallFollowingLab.rightMotor.setSpeed(motorLow);
    		WallFollowingLab.rightMotor.forward();
    		WallFollowingLab.leftMotor.forward();
    		spinning = false;
    	}
    }
    else { //Too far
        WallFollowingLab.leftMotor.setSpeed(motorLow);
        WallFollowingLab.rightMotor.setSpeed(motorHigh);
        WallFollowingLab.rightMotor.forward();
        WallFollowingLab.leftMotor.forward();
        spinning = false;
    }
  }

  @Override
  public int readUSDistance() {
    return this.distance;
  }
}
