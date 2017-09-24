// Lab2.java

package ca.mcgill.ecse211.odometerlab;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class OdometryLab {

  private static final EV3LargeRegulatedMotor leftMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
  
  private static final EV3LargeRegulatedMotor rightMotor =
      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
  
  private static final Port colorPort = LocalEV3.get().getPort("S1");
  
  public static Odometer odometer;
  
  	//These variables are used when we use the correction since our
  	//estimated startinng position is (-15, -15)
	private static final int START_X_ESTIMATE = -15;
	private static final int START_Y_ESTIMATE = -15;
  

  public static final double WHEEL_RADIUS = 2.1;
  public static final double TRACK = 14.1;

  public static void main(String[] args) {
    int buttonChoice;

    final TextLCD t = LocalEV3.get().getTextLCD();
    odometer = new Odometer(leftMotor, rightMotor);
    OdometryDisplay odometryDisplay = new OdometryDisplay(odometer, t);
    
    @SuppressWarnings("resource") // Because we don't bother to close this resource
    SensorModes colorSensor = new EV3ColorSensor(colorPort); // colorSensor is the instance
    SampleProvider colorRed = colorSensor.getMode("Red"); // colorRed provides samples from this instance
    float[] colorRedData = new float[colorRed.sampleSize()]; // colorRedData is the buffer in which data are returned
    
    OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, colorRed, colorRedData);

    do {
      // clear the display
      t.clear();

      // ask the user whether the motors should drive in a square or float
      t.drawString("< Left | Right >", 0, 0);
      t.drawString("       |        ", 0, 1);
      t.drawString(" Float | Drive  ", 0, 2);
      t.drawString("motors | in a   ", 0, 3);
      t.drawString("       | square ", 0, 4);

      buttonChoice = Button.waitForAnyPress();
    } while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);

    if (buttonChoice == Button.ID_LEFT) {

      leftMotor.forward();
      leftMotor.flt();
      rightMotor.forward();
      rightMotor.flt();

      odometer.start();
      odometryDisplay.start();

    } else {
      // clear the display
      t.clear();

      // ask the user whether the motors should drive in a square or float
      t.drawString("< Left | Right >", 0, 0);
      t.drawString("  No   | with   ", 0, 1);
      t.drawString(" corr- | corr-  ", 0, 2);
      t.drawString(" ection| ection ", 0, 3);
      t.drawString("       |        ", 0, 4);
      
      buttonChoice = Button.waitForAnyPress();
      
      odometer.start();
      odometryDisplay.start();
      
      if(buttonChoice == Button.ID_RIGHT) {
    	odometer.setX(START_X_ESTIMATE);
    	odometer.setY(START_Y_ESTIMATE);
        odometryCorrection.start();
      }
      
      // spawn a new Thread to avoid SquareDriver.drive() from blocking
      (new Thread() {
        public void run() {
          SquareDriver.drive(leftMotor, rightMotor, WHEEL_RADIUS, WHEEL_RADIUS, TRACK);
        }
      }).start();
    }

    while (Button.waitForAnyPress() != Button.ID_ESCAPE);
    System.exit(0);
  }
}
