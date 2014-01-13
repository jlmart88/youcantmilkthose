package team154.roles;

/**
 * This class contains the information for the different roles
 * 
 * For now, the roles only have their related communicationID, but we can eventually
 * store role specific data here.
 * 
 * @author jlmart88
 *
 */
public enum RobotRoles {

	     // COMM_ID   IDEAL_NUM
SCOUT       (0, 	  0),
CONSTRUCTOR (1, 	  4),
COWBOY      (2, 	  4),
SOLDIER     (3, 	  12),
DEFENDER    (4, 	  0),
HQ          (5, 	  1),

;

/**
* The ID to use in broadcasting
*/
public final int communicationID;
public final int idealNum;

RobotRoles(int communicationID, int idealNum) {
  this.communicationID = communicationID;
  this.idealNum = idealNum;
}



}


