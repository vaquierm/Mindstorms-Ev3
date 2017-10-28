/**
 * GameParameters.java
 */

package ca.mcgill.ecse211.capturetheflag;

/**
 * GameParameters contains all the specifications sent to the system at the start of a round
 * @author Michael Vaquier
 *
 */

public class GameParameters {
	public final int RedTeam; // (i=1,20) Team starting out from red zone
	public final int GreenTeam; // (i=1,20) Team starting out from green zone
	public final int RedCorner; // (i=1,4) Starting corner for red team
	public final int GreenCorner; // (i=1,4) Starting corner for green team
	public final int OG; // (i=1,5) color of green opponent flag
	public final int OR; // (i=1,5) color of red opponent flag
	public final Coordinate Red_LL; //  lower left hand corner of Red Zone
	public final Coordinate Red_UR; //  upper right hand corner of Red Zone
	public final Coordinate Green_LL; // lower left hand corner of Green Zone 
	public final Coordinate Green_UR; // upper right hand corner of Green Zone
	public final Coordinate ZC_R; // center coordinate of tower in Red Zone
	public final Coordinate ZO_R; // grid crossing adjacent to tower in Red Zone
	public final Coordinate ZC_G; // center coordinate of tower in Green Zone
	public final Coordinate ZO_G; // grid crossing adjacent to tower in Green Zone
	public final Coordinate SH_LL; // lower left hand corner of horizontal shallow water zone
	public final Coordinate SH_UR; // upper right hand corner of horizontal shallow water zone
	public final Coordinate SV_LL; // lower left hand corner of vertical shallow water zone
	public final Coordinate SV_UR; // upper right hand corner of vertical shallow water zone
	public final Coordinate SR_LL; // lower left hand corner of search region in red player zone
	public final Coordinate SR_UR; // upper right hand corner of search region in red player zone
	public final Coordinate SG_LL; // lower left hand corner of search region in green player zone
	public final Coordinate SG_UR; // upper right hand corner of search region in green player zone
	
	public GameParameters(int redTeam, int greenTeam, int redCorner, int greenCorner, int OG, int OR, 
			Coordinate Red_LL, Coordinate Red_UR, Coordinate Green_LL, Coordinate Green_UR,
			Coordinate ZC_R, Coordinate ZO_R, Coordinate ZC_G, Coordinate ZO_G,
			Coordinate SH_LL, Coordinate SH_UR, Coordinate SV_LL, Coordinate SV_UR,
			Coordinate SR_LL, Coordinate SR_UR, Coordinate SG_LL, Coordinate SG_UR) {
		this.RedTeam = redTeam;
		this.GreenTeam = greenTeam;
		this.RedCorner = redCorner;
		this.GreenCorner = greenCorner;
		this.OG = OG;
		this.OR = OR;
		this.Red_LL = Red_LL;
		this.Red_UR = Red_UR;
		this.Green_LL = Green_LL;
		this.Green_UR = Green_UR;
		this.ZC_G = ZC_G;
		this.ZO_G = ZO_G;
		this.ZC_R = ZC_G;
		this.ZO_R = ZO_R;
		this.SH_LL = SH_LL;
		this.SH_UR = SH_UR;
		this.SV_LL = SV_LL;
		this.SV_UR = SV_UR;
		this.SR_LL = SR_LL;
		this.SR_UR = SR_UR;
		this.SG_LL = SG_LL;
		this.SG_UR = SG_UR;
	}

}