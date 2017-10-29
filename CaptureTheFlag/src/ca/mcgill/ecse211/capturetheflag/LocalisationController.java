/**
 * LocalisationController.java
 */

package ca.mcgill.ecse211.capturetheflag;


/**
 * The localisation manager holds the control flow to the localisation procedure
 * It holds a reference to an instance of a localisation class which hold the specific logic for subtasks of the localisation procedure
 * A protocol state machine is used to control the flow and current state of the procedure.
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class LocalisationController implements Runnable {
	
	//Lock objects for threading
	private Object stateLock = new Object();
	private Object pauseLock = new Object();
	private volatile boolean paused = false;
	
	//Associations
	private Localisation localisation;
	private Navigation navigation;
	
	//Game board constants
	private final double tile;
	private final int startingCorner;
	private final int boardSize;
	
	public enum LocalisationState { FULLY_LOCALIZED, DIRECTION_LOCALIZED, UNLOCALIZED }
	
	private LocalisationState localisationState = LocalisationState.UNLOCALIZED;
	
	public LocalisationController(Localisation localisation, Navigation navigation, double tile, int startingCorner, int boardSize) {
		this.localisation = localisation;
		this.navigation = navigation;
		this.tile = tile;
		this.startingCorner = startingCorner;
		this.boardSize = boardSize;
	}
	
	/*
	 * This method executes a full procedure to localise the robot with the assumption
	 * that it starts on the 45° line in the negative quadrant.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		initialLocalisationRoutine();
		while (true) {
			pauseThread();
			colorLocalisationRoutine();
		}
	}
	
	private void initialLocalisationRoutine() {
		localisation.usLocalisation();
		navigateToInitialIntersection();
		localisation.colorLocalisation();
	}
	
	private void navigateToInitialIntersection() {
		switch(startingCorner) {
		case 0:
			navigation.travelTo(tile, tile, false);
			break;
		case 1:
			navigation.travelTo(((boardSize - 1) * tile), tile, false);
			break;
		case 2:
			navigation.travelTo(((boardSize - 1) * tile), ((boardSize - 1) * tile), false);
			break;
		case 3:
			navigation.travelTo(tile, ((boardSize - 1) * tile), false);
			break;
		default:
			break;
		}
	}
	
	private void colorLocalisationRoutine() {
		localisation.colorLocalisation();
	}

	public void setLocalisationState(LocalisationState state) {
		synchronized(stateLock) {
			this.localisationState = state;
		}
	}
	
	public LocalisationState getLocalisationState() {
		synchronized(stateLock) {
			return localisationState;
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

