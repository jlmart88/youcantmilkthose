package team154;

import team154.roles.RobotRoles;

/**
 * This class provides the definitions for how robots will broadcast and read to the
 * global channels
 * 
 * @author jlmart88
 *
 */
public class CommunicationProtocol {
	//channel definitions
	
	//the default spacer to use between data on broadcasts
	public static final int DEFAULT_SPACER = 101;
	
	/**roleChannels display what role each robot should play
	 * 
	 * exist on the range 100-124 
	 * format for channel data: (robotID)(defaultSpacer)(robotRole)
	 */
	public static final int ROLE_CHANNEL_MIN = 100;
	public static final int ROLE_CHANNEL_MAX = 124;
	public static final int ROLE_CHANNELS[] = range(ROLE_CHANNEL_MIN,ROLE_CHANNEL_MAX+1);
	public static final int ROLE_CHANNEL_NUM = ROLE_CHANNELS.length;	
	
	/** Takes in the data stored in a channel and returns what role the channel data is displaying
	 * 
	 * If the channelData is not properly formatted for displaying a role, it will default to returning
	 * RobotRoles.CONSTRUCTOR
	 * 
	 * @param channelData the integer stored in the data
	 * @return RobotRoles the role the channel is displaying
	 */
	public static RobotRoles dataToRole(int channelData){
		//take the last digit of the data, since roles are only one digit
		int communicationID = channelData%10;
		for (RobotRoles role: RobotRoles.values()){
			if (role.communicationID == communicationID){
				return role;
			}
		}
		// if we get here, then the channel data was improperly formatted,
		// so default to returning the Constructor role
		return RobotRoles.CONSTRUCTOR;
	}
	
	/** Takes in the data stored in a channel and returns what robotID the channel data is displaying
	 * 
	 * If the channelData is not properly formatted for displaying the robotID, then will default to returning
	 * -1
	 * 
	 * BYTECODE COST: 20
	 * 
	 * @param channelData the integer stored in the data
	 * @return RobotRoles the role the channel is displaying
	 */
	public static int dataToRobotID(int channelData){
		String dataString = Integer.toString(channelData);
		int spacerLocation = dataString.lastIndexOf(Integer.toString(DEFAULT_SPACER));
		if (spacerLocation != -1){
			return Integer.parseInt(dataString.substring(0, spacerLocation));
		}
		// if we get here, then the channel data was improperly formatted,
		// so default to returning -1
		return -1;
	}
	
	/** Takes in a robotID and a robotRole and returns the corresponding channelData
	 * 
	 * 
	 * @param robotID integer of the robot's id
	 * @param role RobotRoles the robot should have
	 * @return int the channel data to broadcast
	 */
	public static int roleToData(int robotID, RobotRoles role){
		return Integer.parseInt(Integer.toString(robotID)+
				Integer.toString(DEFAULT_SPACER)+Integer.toString(role.communicationID));	
	}
	
	//returns an int[] array where 'begin' is inclusive, e'nd' is exclusive
	//ex: range(3,6) ---> [3,4,5]
	private static int[] range(int begin, int end){
		int out[] = new int[end-begin];
		for (int i=0;i<end-begin;i++){
			out[i]=begin+i;
		}
		return out;
	}
}
