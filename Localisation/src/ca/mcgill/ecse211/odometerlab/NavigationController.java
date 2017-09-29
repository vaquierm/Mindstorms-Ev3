package ca.mcgill.ecse211.odometerlab;

import java.util.List;

public class NavigationController extends Thread {
	private static final NavigationState NAVIGATING = null;

	private static Object lock = new Object();
	
	private UltrasonicPoller usPoller;
	private Odometer odometer;
	private Navigation navigation;
	private List<Coordinate> coordinateList;
	private final boolean objectDetection;
	private static NavigationState state = NavigationState.NAVIGATING;
	
	public enum NavigationState { NAVIGATING, AVOIDING }
	
	public NavigationController(UltrasonicPoller usPoller, Odometer odometer, Navigation navigation, List<Coordinate> coordinateList, boolean objectDetection) {
		this.usPoller = usPoller;
		this.odometer = odometer;
		this.navigation = navigation;
		this.coordinateList = coordinateList;
		this.objectDetection = objectDetection;
		
		odometer.start();
		if (this.objectDetection) {
			this.usPoller.start();
		}
		
	}
	
	public void run() {
		while(coordinateList.size() > 0) {
			
			switch(getNavigationState()) {
			case NAVIGATING:
				Coordinate point = coordinateList.get(0);
				
				navigation.travelTo(point.x, point.y);
				
				double x = odometer.getX();
				double y = odometer.getY();
				
				if(Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)) < 3) { //if the robot was not interrupted and roughly made it to its destination we can remove the next point to travel to.
					coordinateList.remove(0);
				}
				break;
			case AVOIDING:
				int interrupted = navigation.getInterruptedTheta();
				int current = (int) odometer.getThetaDegrees();
				int difference = Math.abs(interrupted - current);
				if(difference < 185 && difference > 175) {
					setNavigationState(NAVIGATING);
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					
				}
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