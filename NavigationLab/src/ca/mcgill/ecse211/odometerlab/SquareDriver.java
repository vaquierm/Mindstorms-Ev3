/*
 * SquareDriver.java
 */
package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class SquareDriver {
  private static final int FORWARD_SPEED = 250;
  private static final int ROTATE_SPEED = 150;

  public static void drive(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
      double leftRadius, double rightRadius, double width) {
    // reset the motors
    for (EV3LargeRegulatedMotor motor : new EV3LargeRegulatedMotor[] {leftMotor, rightMotor}) {
      motor.stop();
      motor.setAcceleration(250);
    }

    // wait 5 seconds
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      // there is nothing to be done here because it is not expected that
      // the odometer will be interrupted by another thread
    }

    for (int i = 0; i < 4; i++) {
      // drive forward two tiles
      leftMotor.setSpeed(FORWARD_SPEED);
      rightMotor.setSpeed(FORWARD_SPEED);

      leftMotor.rotate(convertDistance(leftRadius, 90), true);
      rightMotor.rotate(convertDistance(rightRadius, 90), false);

      // turn 90 degrees clockwise
      leftMotor.setSpeed(ROTATE_SPEED);
      rightMotor.setSpeed(ROTATE_SPEED);
      
      /*
       * Since the right motor was just rotating forward, the next degree in the opposite direction seem to not create any torque and the
       * robot would jerk and skid, making the direction innacuate.
       * To correct that we make the right wheel rotate one degree forward then update that in the odometer and wait a small delay to ensure that the 
       * odometer is able to take that change into account.
       */
      leftMotor.rotate(-convertAngle(leftRadius, width, 1.0), false);
      NavigationLab.odometer.setLeftMotorTachoCount(NavigationLab.odometer.getLeftMotorTachoCount() - 1);
      try {
    	  Thread.sleep(100);
      } catch (InterruptedException e) {
    	  
      }

      leftMotor.rotate(-convertAngle(leftRadius, width, 90.0), true);
      rightMotor.rotate(convertAngle(rightRadius, width, 90.0), false);
      
      /*
       * Since the right motor was just rotating backwards, the next degree in the opposite direction seem to not create any torque and the
       * robot would jerk and skid, making the direction innacuate.
       * To correct that we make the right wheel rotate one degree forward then update that in the odometer and wait a small delay to ensure that the 
       * odometer is able to take that change into account.
       */
      leftMotor.rotate(convertAngle(leftRadius, width, 1.0), false);
      NavigationLab.odometer.setLeftMotorTachoCount(NavigationLab.odometer.getLeftMotorTachoCount() + 1);
      try {
    	  Thread.sleep(100);
      } catch (InterruptedException e) {
    	  
      }
    }
  }

  public static int convertDistance(double radius, double distance) {
    return (int) ((180.0 * distance) / (Math.PI * radius));
  }

  public static int convertAngle(double radius, double width, double angle) {
    return convertDistance(radius, Math.PI * width * angle / 360.0);
  }
}
