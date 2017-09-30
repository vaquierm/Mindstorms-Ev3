package ca.mcgill.ecse211.odometerlab;

import ca.mcgill.ecse211.odometerlab.NavigationController.NavigationState;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * Control of the wall follower is applied periodically by the UltrasonicPoller thread. The while
 * loop at the bottom executes in a loop. Assuming that the us.fetchSample, and cont.processUSData
 * methods operate in about 20mS, and that the thread sleeps for 50 mS at the end of each loop, then
 * one cycle through the loop is approximately 70 mS. This corresponds to a sampling rate of 1/70mS
 * or about 14 Hz.
 */
public class UltrasonicPoller extends Thread {
  private SampleProvider us;
  private UltrasonicController cont;
  private float[] usData;
  
  private Navigation navigation;
  
  private static final int OBSTACLE_THRESHOLD = 15;
  
  private EV3LargeRegulatedMotor USMotor =
	      new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
  

  public UltrasonicPoller(Navigation navigation, SampleProvider us, float[] usData, UltrasonicController cont) {
    this.us = us;
    this.cont = cont;
    this.usData = usData;
    this.navigation = navigation;
    USMotor.setSpeed(30);
  }

  /*
   * Sensors now return floats using a uniform protocol. Need to convert US result to an integer
   * [0,255] (non-Javadoc)
   * 
   * @see java.lang.Thread#run()
   */
  public void run() {
    int distance;
    while (true) {
      us.fetchSample(usData, 0); // acquire data
      distance = (int) (usData[0] * 100.0); // extract from buffer, cast to int
      
      switch (NavigationController.getNavigationState()) {
      case NAVIGATING:
    	  if(distance < OBSTACLE_THRESHOLD) { //an object was detected in front of  the robot, turn sensor towards the wall as the robot rotates
        	  USMotor.rotate(-70, true);
        	  navigation.interruptNav();
        	  NavigationController.setNavigationState(NavigationState.AVOIDING);
          }
    	  break;
      case AVOIDING:
    	  cont.processUSData(distance); // now take action depending on value
      }
     
      
      try {
        Thread.sleep(50);
      } catch (Exception e) {
      } // Poor man's timed sampling
    }
  }
  
public void usSensorStraight() {
	USMotor.rotate(70, true);
}
  

}
