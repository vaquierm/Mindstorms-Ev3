package ca.mcgill.ecse211.zipline;

import lejos.hardware.Button;
import lejos.hardware.Sound;

/**
 * The localisation manager holds the control flow to the localisation procedure
 * It holds a reference to an instance of a localisation class which hold the specific logic for subtasks of the localisation procedure
 * A protocol state machine is used to control the flow and current state of the procedure.
 * 
 * @author Michael Vaquier
 * @author Oliver Clark
 */

public class LocalisationManager extends Thread {
	
	public enum LocalisationState { FULLY_LOCALIZED, DIRECTION_LOCALIZED, UNLOCALIZED }
	
	private Object lock = new Object();
	
	private LocalisationState localisationState = LocalisationState.UNLOCALIZED;
	private UltrasonicPoller usPoller;
	private ColorPoller colorPoller;
	private Navigation nav;
	private Localisation localisation;
	private boolean fallingEdge = false;
	
	public LocalisationManager(UltrasonicPoller usPoller, ColorPoller colorPoller,Navigation nav, boolean fallingEdge) {
		this.usPoller = usPoller;
		this.fallingEdge = fallingEdge;
		this.colorPoller = colorPoller;
		this.nav = nav;
		this.localisation = new Localisation(usPoller, colorPoller, this.fallingEdge);
		usPoller.setLocalisation(this.localisation);
		usPoller.setFallingEdgeMode(this.fallingEdge);
		colorPoller.setLocalisation(this.localisation);
	}
	
	/*
	 * This method executes a full procedure to localise the robot with the assumption
	 * that it starts on the 45° line in the negative quadrant.
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (getLocalisationState() != LocalisationState.FULLY_LOCALIZED) {
			switch(getLocalisationState()) {
			case UNLOCALIZED:
				localisation.alignAngle();
				setLocalisationState(LocalisationState.DIRECTION_LOCALIZED);
				
				break;
			case DIRECTION_LOCALIZED:
				nav.turnTo(45);
				//System.out.println(localisation.edgeDifference);
				//nav.forward(((-14/11) * localisation.edgeDifference) + 233, false);
				nav.forward(9, false);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
				localisation.fixXY();
				setLocalisationState(LocalisationState.FULLY_LOCALIZED);
				break;
			default:
				break;
			}
		}
		nav.travelTo(0, 0, false);
		nav.turnTo(0);
		return;
	}

	public void setLocalisationState(LocalisationState state) {
		synchronized(lock) {
			this.localisationState = state;
		}
	}
	
	public LocalisationState getLocalisationState() {
		synchronized(lock) {
			return localisationState;
		}
	}
}
