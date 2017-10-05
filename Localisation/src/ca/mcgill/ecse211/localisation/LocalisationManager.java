package ca.mcgill.ecse211.localisation;

import lejos.hardware.Button;
import lejos.hardware.Sound;

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
	}
	
	public void run() {
		while (getLocalisationState() != LocalisationState.FULLY_LOCALIZED) {
			switch(getLocalisationState()) {
			case UNLOCALIZED:
				localisation.alignAngle();
				setLocalisationState(LocalisationState.DIRECTION_LOCALIZED);
				nav.turnTo(0);
				break;
			case DIRECTION_LOCALIZED:
				while(Button.waitForAnyPress() != Button.ID_ENTER) {
					
				}
				localisation.fixXY();
				break;
			default:
				break;
			}
		}
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
