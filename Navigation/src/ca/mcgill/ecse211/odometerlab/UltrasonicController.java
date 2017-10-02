package ca.mcgill.ecse211.odometerlab;

/**
 * 
 * reads Ultrasonic sensor
 * 
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public interface UltrasonicController {

  public void processUSData(int distance);

  public int readUSDistance();
}
