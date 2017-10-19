package ca.mcgill.ecse211.capturetheflag;

/**
 * 
 * Navigation class which navigates robot to specified points, 
 * determines direction to turn (Minimal turn) and how far to travel
 * 
 * @author Oliver Clark
 * @author Michael Vaquier
 */
public class Navigation {
		
	private static final int FORWARD_SPEED = 150;
	private static final int ROTATE_SPEED = 140;

	private static final int SLOW_ACCEL = 250;
	private static final int FAST_ACCEL = 500;
	

	public Navigation() {

	}

	/*
	 * This method causes the robot to travel to the absolute field location (x,
	 * y), specified in tilepoints.This method should continuously
	 * callturnTo(double theta)and thenset the motor speed to forward(straight).
	 * This will make sure that yourheading is updated until you reach your
	 * exact goal. This method will pollthe MainController.odometer for informatio
	 */
	public void travelTo(double x, double y, boolean returnThread) {		
		
		double currentX = MainController.odometer.getX();
		double currentY = MainController.odometer.getY();
		
		double nextHeading = Math.toDegrees(Math.atan2(x - currentX, y - currentY));

		turnTo(nextHeading);
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			
		}
	    
	    double distance = Math.sqrt(Math.pow((y - currentY),2) + Math.pow((x - currentX),2));
	    
	    forward(distance, returnThread);
	    
	   
	}
	
	public void turnTo(double theta) {
		MainController.leftMotor.setSpeed(ROTATE_SPEED);
	    MainController.rightMotor.setSpeed(ROTATE_SPEED);
		double currentTheta = MainController.odometer.getThetaDegrees();
		double rightRotation = theta - currentTheta;
		if (rightRotation < 0) {
			rightRotation = rightRotation + 360;
		}
		double leftRotation = currentTheta - theta;
		if (leftRotation < 0) {
			leftRotation = leftRotation + 360;
		}
		MainController.rightMotor.setAcceleration(SLOW_ACCEL);
		MainController.leftMotor.setAcceleration(SLOW_ACCEL);
		if (rightRotation < leftRotation) {
			MainController.leftMotor.rotate(convertAngle(MainController.WHEEL_RADIUS, MainController.TRACK, rightRotation), true);
		    MainController.rightMotor.rotate(-convertAngle(MainController.WHEEL_RADIUS, MainController.TRACK, rightRotation), false);
		}
		else {
			MainController.leftMotor.rotate(-convertAngle(MainController.WHEEL_RADIUS, MainController.TRACK, leftRotation), true);
		    MainController.rightMotor.rotate(convertAngle(MainController.WHEEL_RADIUS, MainController.TRACK, leftRotation), false);
		}
		MainController.rightMotor.setAcceleration(FAST_ACCEL);
		MainController.leftMotor.setAcceleration(FAST_ACCEL);
	}
	
	public void forward(double distance, boolean returnThread) {
		MainController.leftMotor.setSpeed(FORWARD_SPEED);
	    MainController.rightMotor.setSpeed(FORWARD_SPEED);
		MainController.leftMotor.rotate(convertDistance(MainController.WHEEL_RADIUS, distance), true);
	    MainController.rightMotor.rotate(convertDistance(MainController.WHEEL_RADIUS, distance), returnThread);
	}


	public int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	public int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

	
	
}
