package team154;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer{
	
	public static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	
	//constants for assigning roles
	static RobotRole myRole = null;
	static int currentRoleChannel = 0;
	static int lastSpawnedID = 0;
	static boolean justSpawned = false;
	
	public static void run(RobotController rcin){
		rc = rcin;
		randall.setSeed(rc.getRobot().getID());
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){//if I'm a headquarters
					runHeadquarters();
				}else if(rc.getType()==RobotType.SOLDIER){
					runSoldier();
				}
				rc.yield();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	private static void runSoldier() throws GameActionException {
		
		//if we dont have a role, sift through the roleChannels to try to find one
		if (myRole == null){
			for (int channel:CommunicationProtocol.ROLE_CHANNELS){
				int channelData = rc.readBroadcast(channel);
				if (CommunicationProtocol.dataToRobotID(channelData)==rc.getRobot().getID()){
					myRole = CommunicationProtocol.dataToRole(channelData);
					rc.setIndicatorString(1, "My role is: "+myRole.name());
				}	
			}
		}
		
		
		int currentBytecode = Clock.getBytecodeNum();
		tryToShoot();
		//communication
		//rc.setIndicatorString(0, "read ID: "+rc.readBroadcast(0));
		int editingChannel = (Clock.getRoundNum()%2);
		int usingChannel = ((Clock.getRoundNum()+1)%2);
		
		int runningTotal = rc.readBroadcast(editingChannel);
		rc.broadcast(editingChannel, runningTotal+1);
		
		MapLocation runningVectorTotal = intToLoc(rc.readBroadcast(editingChannel+2));
		rc.broadcast(editingChannel+2,locToInt(mladd(runningVectorTotal,rc.getLocation())));
		MapLocation averagePositionOfSwarm = mldivide(intToLoc(rc.readBroadcast(usingChannel+2)),rc.readBroadcast(usingChannel));
		
		rc.setIndicatorString(0, ""+locToInt(averagePositionOfSwarm));
		
		//movement
//		Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
//		if(rc.isActive()&&rc.canMove(chosenDirection)){
//			rc.move(chosenDirection);
//		}
		swarmMove(averagePositionOfSwarm);
	}
	
	private static void swarmMove(MapLocation averagePositionOfSwarm) throws GameActionException{
		Direction chosenDirection = rc.getLocation().directionTo(averagePositionOfSwarm);
		if(rc.isActive()){
			if(randall.nextDouble()<0.5){//go to swarm center
				for(int directionalOffset:directionalLooks){
					int forwardInt = chosenDirection.ordinal();
					Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
					if(rc.canMove(trialDir)){
						rc.move(trialDir);
						break;
					}
				}
			}else{//go wherever the wind takes you
				Direction d = allDirections[(int)(randall.nextDouble()*8)];
				if(rc.isActive()&&rc.canMove(d)){
					rc.move(d);
				}
			}
		}
	}
	
	private static MapLocation mladd(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	private static MapLocation mldivide(MapLocation bigM, int divisor){
		return new MapLocation(bigM.x/(divisor+1), bigM.y/(divisor+1));
	}

	private static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	private static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	
	private static void tryToShoot() throws GameActionException {
		//shooting
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if(enemyRobots.length>0){//if there are enemies
			Robot anEnemy = enemyRobots[0];
			RobotInfo anEnemyInfo;
			anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
				if(rc.isActive()){
					rc.attackSquare(anEnemyInfo.location);
				}
			}
		}else{//there are no enemies, so build a tower
			if(randall.nextDouble()<0.001&&rc.sensePastrLocations(rc.getTeam()).length<5){
				//rc.senseCowsAtLocation(arg0);
				if(rc.isActive()){
					rc.construct(RobotType.PASTR);
				}
			}
		}
	}

	private static void runHeadquarters() throws GameActionException {
		
		//give myself the role of HQ
		if (myRole == null){
			myRole = RobotRole.HQ;
		}
		
		Direction spawnDir = Direction.NORTH;
		if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			rc.spawn(Direction.NORTH);
			justSpawned = true;//tell the HQ to try to broadcast role information
		}
		
		if(justSpawned){//try to find our most recent spawn, and broadcast its role info
			Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, 9, rc.getTeam());
			for (Robot spawnedRobot:nearbyRobots){
				int spawnedID = spawnedRobot.getID();
				if(spawnedID>lastSpawnedID){//our next spawn will have a greater id than the last spawn
					//for now, randomly choose a role
					RobotRole role = RobotRole.values()[(int)(randall.nextDouble()*10)%4];
					
					rc.broadcast(CommunicationProtocol.ROLE_CHANNELS[currentRoleChannel], 
								CommunicationProtocol.roleToData(spawnedRobot.getID(), role));
					
					justSpawned = false; //reset the trigger for broadcasting roles
					
					//increment/loop role channel
					currentRoleChannel = (currentRoleChannel+1)%CommunicationProtocol.ROLE_CHANNEL_NUM; 
					
					lastSpawnedID=spawnedID;//track the most recent spawn
					
					rc.setIndicatorString(0, "Just Spawned a "+role.name()+" with ID: "+lastSpawnedID);
				}
			}
		}
		
		int editingChannel = (Clock.getRoundNum()%2);
		rc.broadcast(editingChannel, 0);
		rc.broadcast(editingChannel+2, 0);
	}
}