package ca.mcgill.ecse211.odometerlab;

import java.util.List;

public class NavigationController extends Thread {

	private static Object lock = new Object();
	
	private UltrasonicPoller usPoller;
	private Odometer odometer;
	private Navigation navigation;
	private List<Coordinate> coordinateList;
	private final boolean objectDetection;
	private static NavigationState state = NavigationState.READY;
	
	public enum NavigationState { NAVIGATING, AVOIDING, READY }
	
	public NavigationController(UltrasonicPoller usPoller, Odometer odometer, Navigation navigation, List<Coordinate> coordinateList, boolean objectDetection) {
		this.usPoller = usPoller;
		this.odometer = odometer;
		this.navigation = navigation;
		this.coordinateList = coordinateList;
		this.objectDetection = objectDetection;
		
		navigation.setController(this);
		
		odometer.start();
		if (this.objectDetection) {
			this.usPoller.start();
		}
		
	}
	
	public void run() {

		while(coordinateList.size() > 0) {
			Coordinate point = coordinateList.get(0);
			switch(getNavigationState()) {
			case READY:
				
				navigation.travelTo(point.x, point.y);
				setNavigationState(NavigationState.NAVIGATING);
				break;
			case NAVIGATING:
				
				double x = odometer.getX();
				double y = odometer.getY();
				
				if(Math.sqrt(Math.pow(x - point.x, 2)+Math.pow(y - point.y, 2)) < 1.5) { //if the robot was not interrupted and roughly made it to its destination we can remove the next point to travel to.
					coordinateList.remove(0);
					setNavigationState(NavigationState.READY);
				}
				
				break;
			case AVOIDING:
				int interrupted = navigation.getInterruptedTheta();
				int current = (int) odometer.getThetaDegrees();
				int difference = Math.abs(interrupted - current);
				//System.out.println(difference);
				if(difference < 195 && difference > 165) { //when the robot is pointing to the opposite direction after it was interrupted, we can start navigating again.
					setNavigationState(NavigationState.READY);
					usPoller.usSensorStraight();
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