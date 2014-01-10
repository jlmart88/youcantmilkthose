package team154;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

import team154.BasicPathing;
import team154.BreadthFirst;
import team154.VectorFunctions;

public class RobotPlayer{
    
    public static RobotController rc;
    static Direction allDirections[] = Direction.values();
    static Random randall = new Random();
    static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
    static boolean mapCreated = false;
	static ArrayList<MapLocation> path;
	static int bigBoxSize = 5;

	//constants for assigning roles
	static RobotRole myRole = null;
	static int currentRoleChannel = 0;
	static int lastSpawnedID = 0;
	static boolean justSpawned = false;

    
    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
		if(rc.getType()==RobotType.HQ){
		}else{
			BreadthFirst.init(rc, bigBoxSize);
			MapLocation goal = rc.senseEnemyHQLocation();
			path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
			//VectorFunctions.printPath(path,bigBoxSize);
		}
        while(true){
            try{
                if(rc.getType()==RobotType.HQ){//if I'm a headquarters
                    runHeadquarters(height,width);
                }else if(rc.getType()==RobotType.SOLDIER){
                    runSoldier(height, width);
                }
                rc.yield();
            }catch (Exception e){
//                e.printStackTrace();
            }
        }
    }

    private static void runSoldier(int height, int width) throws GameActionException {
    	
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

        Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
        boolean HQdetected = false;
        if(enemyRobots.length>0){//if there are enemies
			rc.setIndicatorString(0, "There are enemies");
        	int locationSize = enemyRobots.length;
			for(int i=0;i<enemyRobots.length;i++){
				Robot anEnemy = enemyRobots[i];
				RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
				if(anEnemyInfo.type == RobotType.HQ){
					HQdetected = true;
				}
			}
			if(locationSize != 1 || HQdetected == false){
				MapLocation[] robotLocations = new MapLocation[locationSize];
				for(int i=0;i<locationSize;i++){
					Robot anEnemy = enemyRobots[i];
					RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
					if(anEnemyInfo.type != RobotType.HQ){
						robotLocations[i] = anEnemyInfo.location;
					}
					else{
						robotLocations[i] = new MapLocation(10000,10000);
					}
				}
				System.out.println(robotLocations[0]);
				MapLocation closestEnemyLoc = VectorFunctions.findClosest(robotLocations, rc.getLocation());

				if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
					rc.setIndicatorString(1, "trying to shoot");
					if(rc.isActive()){
						rc.attackSquare(closestEnemyLoc);
					}
				}else{
					rc.setIndicatorString(1, "trying to go closer");
					Direction towardClosest = rc.getLocation().directionTo(closestEnemyLoc);
					simpleMove(towardClosest);
				}
			}
			else if(locationSize == 1 && HQdetected == true){
				Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
				if(rc.isActive()&&rc.canMove(chosenDirection)){
					rc.move(chosenDirection);
				}
			}
			else{
				if(path.size()==0){
					path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(rc.senseEnemyHQLocation(),bigBoxSize), 100000);
				}
				//follow breadthFirst path
				Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
				BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);

			}
        }else{
			if(path.size()==0){
				path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(rc.senseEnemyHQLocation(),bigBoxSize), 100000);
			}
			//follow breadthFirst path
			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
		}
//there are no enemies, so build a tower
//            if(randall.nextDouble()<1 && rc.sensePastrLocations(rc.getTeam()).length<10){
//                if (senseCowsAtRange(rc.getLocation()) > 2500){
//                    if(rc.isActive()){
//                        rc.construct(RobotType.PASTR);
//                    }
//                }
//            }
        //movement
//        Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
//        if(rc.isActive()&&rc.canMove(chosenDirection)){
//            Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class, 5);
//            RobotInfo x;
//            for(Robot robot:nearbyRobots){
//                x = rc.senseRobotInfo(robot);
//                if(x.isConstructing){
//                    rc.sneak(chosenDirection);
//                }
//            }
//            rc.move(chosenDirection);
//        }
//        swarmMove(height, width);
    }
    
    private static void swarmMove(int height, int width) throws GameActionException{
        Direction chosenDirection = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
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
        return new MapLocation(bigM.x/divisor, bigM.y/divisor);
    }

    private static int locToInt(MapLocation m){
        return (m.x*100 + m.y);
    }
    
    private static MapLocation intToLoc(int i){
        return new MapLocation(i/100,i%100);
    }
    
    private static int senseCowsAtRange(MapLocation loc) throws GameActionException{
        int cows = 0;
        MapLocation[] adjLocs = MapLocation.getAllMapLocationsWithinRadiusSq(loc, 5);
        for(MapLocation innerLoc: adjLocs){
            cows += rc.senseCowsAtLocation(innerLoc);
        }
        return cows;
    }

    
	private static void simpleMove(Direction chosenDirection) throws GameActionException{
		for(int directionalOffset:directionalLooks){
			int forwardInt = chosenDirection.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(rc.canMove(trialDir)){
				rc.move(trialDir);
				break;
			}
		}
	}

    
    private static void runHeadquarters(int height, int width) throws GameActionException {
    	
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
        char[][] map = new char[width][height];
        if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
            rc.spawn(Direction.NORTH);
        }
        if(mapCreated == false){ //Create a map of the battlefield
            for(int y=0; y<height; y++){
                for(int x=0; x<width; x++){
                    if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.NORMAL){
                        map[x][y] = '-';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.VOID){
                        map[x][y] = '#';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.OFF_MAP){
                        map[x][y] = '^';
                    }
                    else if(rc.senseTerrainTile(new MapLocation(x,y)) == TerrainTile.ROAD){
                        map[x][y] = '=';
                    }
                    System.out.print(map[x][y]);
                }
                System.out.println();
            }
            mapCreated = true;
        }
//        int editingChannel = (Clock.getRoundNum()%2);
//        int usingChannel = ((Clock.getRoundNum()+1)%2);
//        rc.broadcast(editingChannel, 0);
//        rc.broadcast(editingChannel+2, 0);
    }
}