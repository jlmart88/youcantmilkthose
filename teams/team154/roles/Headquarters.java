package team154.roles;

import java.util.HashMap;
import java.util.Random;

import team154.CommunicationProtocol;
import team154.MapAnalyzer;
import team154.RobotPlayer;
import team154.movement.VectorFunctions;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public class Headquarters {
	
	//constants for assigning roles
    static boolean mapCreated = false;
	static int currentRoleChannel = 0;
	static int lastSpawnedID = 0;
	static boolean justSpawned = false;
	static HashMap<Integer, RobotRoles> currentRolesDict = new HashMap<Integer,RobotRoles>();
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};


	public static void runHeadquarters(int height, int width, RobotController rc) throws GameActionException {

		//attacks nearby enemies
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if(enemyRobots.length>0){
			int locationSize = enemyRobots.length;
			for(int i=0;i<locationSize;i++){
				Robot anEnemy = enemyRobots[i];
				RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
				MapLocation enemyRobotLocation = anEnemyInfo.location;
				if(enemyRobotLocation.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
					rc.setIndicatorString(1, "trying to shoot");
					if(rc.isActive()){
						rc.attackSquare(enemyRobotLocation);
					}
				}
			}
		}
		for(Direction spawnDir:directions){
			if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
				rc.spawn(spawnDir);
				justSpawned = true;//tell the HQ to try to broadcast role information
				break;
			}
		}
		
		if(justSpawned){//try to find our most recent spawn, and broadcast its role info
			Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, 9, rc.getTeam());
			for (Robot spawnedRobot:nearbyRobots){
				int spawnedID = spawnedRobot.getID();
				if(spawnedID>lastSpawnedID){//our next spawn will have a greater id than the last spawn
					
					//for now, randomly choose a role
					//RobotRoles role = RobotRoles.values()[(int)(RobotPlayer.randall.nextDouble()*10)%4];
					
					//assign roles based on ideal numbers in RobotRoles
					RobotRoles role = Headquarters.getNextRole(rc, spawnedID);
					currentRolesDict.put(spawnedID, role);
					
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
		rc.broadcast(20000, -100);
		MapLocation[] enemyPastrLocations = rc.sensePastrLocations(rc.getTeam().opponent());
		if(enemyPastrLocations.length>0){
			MapLocation closestPastr = VectorFunctions.findClosest(enemyPastrLocations, rc.getLocation());
			rc.broadcast(20000, VectorFunctions.locToInt(closestPastr));
		}

        if(mapCreated == false){ //Create a map of the battlefield
        	char[][] map = new char[height][width];
            for(int y=0; y<height; y++){
                for(int x=0; x<width; x++){
                    if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.NORMAL){
                        map[y][x] = '-';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.VOID){
                        map[y][x] = '#';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.OFF_MAP){
                        map[y][x] = '^';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.ROAD){
                        map[y][x] = '=';
                    }
                    System.out.print(map[y][x]);
                }
                System.out.println();
            }
            mapCreated = true;
            
            MapLocation[] idealPastrLocations = MapAnalyzer.findIdealPastrLocations(map,rc.senseCowGrowth(),rc.getLocation(),rc);
            System.out.println("I'm here");
            MapAnalyzer.printIdealPastrLocations(map,idealPastrLocations);
            for(int x=0; x<idealPastrLocations.length; x++){
            	System.out.println(idealPastrLocations[x]);
            	rc.broadcast(CommunicationProtocol.PASTR_LOCATION_CHANNEL_MIN+x, VectorFunctions.locToInt(idealPastrLocations[x]));
            }
        }        
    }


	private static RobotRoles getNextRole(RobotController rc, int spawnedID) {
		Robot[] ourRobots= rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam());
		
		//update the roles dict
		HashMap<Integer, RobotRoles> newRolesDict = new HashMap<Integer,RobotRoles>();
		HashMap<RobotRoles, Integer> rolesCountDict = new HashMap<RobotRoles,Integer>();
		for (RobotRoles role:RobotRoles.values()){
			rolesCountDict.put(role, 0);
		}
		for (Robot robot:ourRobots){
			//System.out.println("here: "+robot.toString());
			int robotID = robot.getID();
			RobotRoles role = currentRolesDict.get(robotID);
			if (role!=null){
				newRolesDict.put(robotID, role);
				int currentNum = rolesCountDict.get(role);
				rolesCountDict.put(role, currentNum+1);
			} else if (robot.getID()!=spawnedID){ //we have a pastr/noise tower
				role=RobotRoles.CONSTRUCTOR;
				newRolesDict.put(robotID, role);
				int currentNum = rolesCountDict.get(role);
				rolesCountDict.put(role, currentNum+1);
			}
		}
		//System.out.println(rolesCountDict);
		//System.out.print(currentRolesDict);
		//System.out.println((RobotRoles.COWBOY.idealNum+RobotRoles.CONSTRUCTOR.idealNum)-
		//		(rolesCountDict.get(RobotRoles.CONSTRUCTOR)+rolesCountDict.get(RobotRoles.COWBOY)));

		currentRolesDict = newRolesDict;
		
		//figure out what we need
		
		if (rolesCountDict.get(RobotRoles.CONSTRUCTOR)==0){
			return RobotRoles.CONSTRUCTOR;
		}
		//Soldier or farmer-related?
		if ((RobotRoles.SOLDIER.idealNum - rolesCountDict.get(RobotRoles.SOLDIER))>
				((RobotRoles.COWBOY.idealNum+RobotRoles.CONSTRUCTOR.idealNum)-
				(rolesCountDict.get(RobotRoles.CONSTRUCTOR)+rolesCountDict.get(RobotRoles.COWBOY)))){
			return RobotRoles.SOLDIER;
		}else{
			//constructor or cowboy?
			if ((RobotRoles.COWBOY.idealNum - rolesCountDict.get(RobotRoles.COWBOY))>
					(RobotRoles.CONSTRUCTOR.idealNum - rolesCountDict.get(RobotRoles.CONSTRUCTOR))){
				return RobotRoles.COWBOY;
			}else {
				return RobotRoles.CONSTRUCTOR;
			}
		}
	}
}
