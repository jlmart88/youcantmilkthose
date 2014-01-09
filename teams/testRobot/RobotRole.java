package testRobot;

/**
 * This class contains the information for the different roles
 * 
 * For now, the roles only have their related communicationID, but we can eventually
 * store role specific data here.
 * 
 * @author jlmart88
 *
 */
public enum RobotRole {

	     // COMM_ID
SCOUT       (0),
CONSTRUCTOR (1),
COWBOY      (2),
SOLDIER     (3),
DEFENDER    (4),
HQ          (5),

;

/**
* The ID to use in broadcasting
*/
public final int communicationID;

RobotRole(int communicationID) {
  this.communicationID = communicationID;
}

}


