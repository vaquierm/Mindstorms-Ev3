package ca.mcgill.ecse211.localisation;

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

	private static Object lock = new Object();
	
	private UltrasonicPoller usPoller;
	private Odometer odometer;
	private Navigation navigation;
	private List<Coordinate> coordinateList;
	private static NavigationState state = NavigationState.READY;
	
	public enum NavigationState { NAVIGATING, AVOIDING, READY }
	
	public NavigationController(UltrasonicPoller usPoller, Odometer odometer, Navigation navigation, List<Coordinate> coordinateList) {
		this.usPoller = usPoller;
		this.odometer = odometer;
		this.navigation = navigation;
		this.coordinateList = coordinateList;
		
		odometer.start();
		
	}
	
	public void run() {

		while(coordinateList.size() > 0) {
			Coordinate point = coordinateList.get(0);
			switch(getNavigationState()) {
			case READY:
				
				navigation.travelTo(point.x, point.y, true);
				setNavigationState(NavigationState.NAVIGATING);
				break;
			case NAVIGATING:
				
				double x = odometer.getX();
				double y = odometer.getY();
				
				if(!navigation.rightMotor.isMoving() && !navigation.leftMotor.isMoving() && Math.sqrt(Math.pow(x - point.x, 2)+Math.pow(y - point.y, 2)) < 5) { //if the robot was not interrupted and roughly made it to its destination we can remove the next point to travel to.
					coordinateList.remove(0);
					setNavigationState(NavigationState.READY);
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
	
	public static NavigationState getNavigationState() {
		synchronized(lock) {
			return state;
		}
	}
	
	public static void setNavigationState(NavigationState navState) {
		synchronized (lock) {
			state = navState;
		}
	}

}