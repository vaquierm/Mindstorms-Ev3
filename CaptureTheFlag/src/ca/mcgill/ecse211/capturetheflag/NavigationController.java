/**
 * NavigationController
 */

package ca.mcgill.ecse211.capturetheflag;

import java.util.List;

/**
 * 
 * NavigationController allows the robot to switch between navigating 
 * point to point using Navigation and wall following around an obstacle to avoid it
 * using PController
 * 
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class NavigationController extends Thread {

	private static Object stateLock = new Object();
	
	private volatile boolean paused = false;
	private Object pauseLock = new Object();
	
	private List<Coordinate> coordinateList;
	private final boolean objectDetection = false;
	private static NavigationState state = NavigationState.READY;
	
	public enum NavigationState { NAVIGATING, AVOIDING, READY }
	
	public NavigationController() {
		
	}
	
	public void run() {
		
		while(true) {
			objectDetection = false;
			pauseThread();
			if(objectDetection) {
				MainController.objectAvoidanceUsPoller.resumeThread();
			}
			
			while(coordinateList.size() > 0) {
				Coordinate point = coordinateList.get(0);
				switch(getNavigationState()) {
				case READY:
					MainController.navigation.travelTo(point.x, point.y, true);
					setNavigationState(NavigationState.NAVIGATING);
					break;
				case NAVIGATING:
					
					double x = MainController.odometer.getX();
					double y = MainController.odometer.getY();
					
					if(!MainController.navigation.rightMotor.isMoving() && !MainController.navigation.leftMotor.isMoving() && Math.sqrt(Math.pow(x - point.x, 2)+Math.pow(y - point.y, 2)) < 5) { //if the robot was not interrupted and roughly made it to its destination we can remove the next point to travel to.
						coordinateList.remove(0);
						setNavigationState(NavigationState.READY);
					}
					
					break;
				case AVOIDING:
					int interrupted = (int) MainController.navigation.getInterruptedTheta();
					int current = (int) MainController.odometer.getThetaDegrees();
					int difference = Math.abs(interrupted - current);
					//System.out.println(difference);
					if(difference < 195 && difference > 165) { //when the robot is pointing to the opposite direction after it was interrupted, we can start navigating again.
						setNavigationState(NavigationState.READY);
						MainController.objectAvoidanceUsPoller.usSensorStraight();
					}
					break;
					default:
						break;
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					
				}
			}
		}
		
	}
	
	public static NavigationState getNavigationState() {
		synchronized(stateLock) {
			return state;
		}
	}
	
	public static void setNavigationState(NavigationState navState) {
		synchronized (stateLock) {
			state = navState;
		}
	}
	
	//pauses the thread
	private void pauseThread() {
		paused = true;
		synchronized (pauseLock) {
            if (paused) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
	}
	
	//resumes the thread
    public void resumeThread() {
        synchronized (pauseLock) {
        	if (paused) {
        		paused = false;
        		pauseLock.notifyAll(); // Unblocks thread
        	}
        }
    }

}