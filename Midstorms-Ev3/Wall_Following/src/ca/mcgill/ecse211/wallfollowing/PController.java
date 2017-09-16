package ca.mcgill.ecse211.wallfollowing;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class PController implements UltrasonicController {

  /* Constants */
  private static final int MOTOR_SPEED = 140;
  private static final int FILTER_OUT = 40;
  private final int ERROR_PROPORTIONALITY = 35;
  private final int CORNER_SPEED_DIFFERENCE = 60;
  private static final int SPIN_SPEED = 150;

  private int spinningCounter;
  private final int bandCenter;
  private final int bandWidth;
  private int wheelSpeedDifference;
  private int distance;
  private int filterControl;
  private boolean spinning = false;

  public PController(int bandCenter, int bandWidth) {
    this.bandCenter = bandCenter;
    this.bandWidth = bandWidth;
    this.filterControl = 0;
    this.spinningCounter = 0; //counts iteration since last time it was spinning

    WallFollowingLab.leftMotor.setSpeed(MOTOR_SPEED); // Initalize motor rolling forward
    WallFollowingLab.rightMotor.setSpeed(MOTOR_SPEED);
    WallFollowingLab.leftMotor.forward();
    WallFollowingLab.rightMotor.forward();
  }

  @Override
  public void processUSData(int distance) {
	  

    // rudimentary filter - toss out invalid samples corresponding to null
    // signal.
    // (n.b. this was not included in the Bang-bang controller, but easily
    // could have).
    //
	if (distance >= 100 && filterControl < FILTER_OUT) {
      // bad value, do not set the distance var, however do increment the
      // filter value
      filterControl++;
      this.distance = bandCenter;
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
	
    if (distance >= 100) {
    	wheelSpeedDifference = CORNER_SPEED_DIFFERENCE;
    }
    else {
    	wheelSpeedDifference = Math.abs(ERROR_PROPORTIONALITY * (bandCenter - distance)/10);
    	if(wheelSpeedDifference > 70)
    		wheelSpeedDifference = 90;
    }
    
    spinningCounter++;
	
    if (Math.abs(this.distance - bandCenter) < bandWidth) { //Sweet spot
    	WallFollowingLab.leftMotor.setSpeed(MOTOR_SPEED);
        WallFollowingLab.rightMotor.setSpeed(MOTOR_SPEED);
        WallFollowingLab.rightMotor.forward();
        WallFollowingLab.leftMotor.forward();
    }
    else if (this.distance < bandCenter) { //Too close
    	if (this.distance < 20) { // if the robot is way too close, it spins
    		WallFollowingLab.leftMotor.setSpeed(SPIN_SPEED);
    		WallFollowingLab.rightMotor.setSpeed(SPIN_SPEED);
    	    WallFollowingLab.rightMotor.backward();
    	    WallFollowingLab.leftMotor.forward();
    	    spinning = true;
    	    spinningCounter = 0;
    	}
    	else {
    		WallFollowingLab.leftMotor.setSpeed(MOTOR_SPEED + wheelSpeedDifference);
    		WallFollowingLab.rightMotor.setSpeed(MOTOR_SPEED - wheelSpeedDifference);
    		WallFollowingLab.rightMotor.forward();
    		WallFollowingLab.leftMotor.forward();
    		spinning = false;
    	}
    }
    else {//if (spinningCounter > 10) { //Too far
        WallFollowingLab.leftMotor.setSpeed(MOTOR_SPEED - wheelSpeedDifference);
        WallFollowingLab.rightMotor.setSpeed(MOTOR_SPEED + wheelSpeedDifference);
        WallFollowingLab.rightMotor.forward();
        WallFollowingLab.leftMotor.forward();
        spinning = false;
    }
    /*else {
    	WallFollowingLab.leftMotor.setSpeed(MOTOR_SPEED);
        WallFollowingLab.rightMotor.setSpeed(MOTOR_SPEED);
        WallFollowingLab.rightMotor.backward();
        WallFollowingLab.leftMotor.backward();
    }*/
      
    
  }


  @Override
  public int readUSDistance() {
    return this.distance;
  }

}
