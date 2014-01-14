package team154;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

import team154.movement.BasicPathing;
import team154.movement.BreadthFirst;
import team154.movement.VectorFunctions;
import team154.roles.Headquarters;
import team154.roles.RobotRoles;

public class RobotPlayer{
    
    public static RobotController rc;
    static Direction allDirections[] = Direction.values();
    static Random randall = new Random();
    static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	static ArrayList<MapLocation> path;
	static int bigBoxSize = 5;
	
	//constants for assigning roles
	static RobotRoles myRole = null;
    
    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        if(rc.getType()!=RobotType.HQ){
        	BreadthFirst.init(rc,  bigBoxSize);
        	MapLocation goal = rc.getLocation();
			path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
        }
        while(true){
            try{
                if(rc.getType()==RobotType.HQ){//if I'm a headquarters
                    Headquarters.runHeadquarters(height,width,rc);
                }else if(rc.getType()==RobotType.SOLDIER){
                    runSoldier(height, width);
                }
                rc.yield();
            }catch (Exception e){
//               e.printStackTrace();
            }
        }
    }

    private static boolean closeEnough(MapLocation loc1, MapLocation loc2, int dist){
    	if(Math.abs(loc1.x - loc2.x) + Math.abs(loc1.y - loc2.y) <= dist){
    		return true;
    	}
    	return false;
    }
    
    private static void tryToConstruct() throws GameActionException{
    	int x = rc.getRobot().getID()%5;
    	MapLocation currentLoc = rc.getLocation();
    	MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(15000+x));
    	rc.setIndicatorString(2, "I WILL CONSTRUCT AT " + pastrLoc);
    	//there are no enemies, so build a tower
    	if(rc.sensePastrLocations(rc.getTeam()).length<10){
    		if (senseCowsAtRange(rc.getLocation()) > 3500 || currentLoc.equals(pastrLoc)){
    			if(rc.isActive()){
    				rc.construct(RobotType.PASTR);
    			}
    		}
    	}
    	if(path.size()==0){
    		path = BreadthFirst.pathTo(VectorFunctions.mldivide(currentLoc,bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    	}
    	//follow breadthFirst path
    	if(!closeEnough(currentLoc,pastrLoc,7)){
    		Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
    		BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
    	}
    	else{
			Direction towardClosest = rc.getLocation().directionTo(pastrLoc);
			simpleMove(towardClosest);
    	}
    }
    
    private static void tryToGather() throws GameActionException{
    	MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam());
    	MapLocation pastrLoc = rc.getLocation();
    	if(pastrLocs.length>0){
    		int x = rc.getRobot().getID()%pastrLocs.length;
    		if(path.size()==0){
    			pastrLoc = pastrLocs[x];
    			path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    		}
    		//follow breadthFirst path
    		if(!closeEnough(rc.getLocation(),pastrLoc,7)){
    			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
    		}
    		else{
    			Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
    			if(rc.isActive()&&rc.canMove(chosenDirection)){
    				rc.move(chosenDirection);
    			}
    		}
    	}
    }
    
    private static void tryToDefend() throws GameActionException{
    	
    }
    
    public static void tryToShoot() throws GameActionException{
    	Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
    	MapLocation HQLocation = rc.senseEnemyHQLocation();
    	if(enemyRobots.length>0){//if there are enemies
    		rc.setIndicatorString(0, "There are enemies");
    		int locationSize = enemyRobots.length;
    		if(!closeEnough(rc.getLocation(), HQLocation,5)){//if there are non HQ enemies
    			MapLocation[] robotLocations = new MapLocation[locationSize];
    			for(int i=0;i<locationSize;i++){
    				Robot anEnemy = enemyRobots[i];
    				RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
    				robotLocations[i] = anEnemyInfo.location;
    			}
    			MapLocation closestEnemyLoc = VectorFunctions.findClosest(robotLocations, rc.getLocation());
    			MapLocation lowestEnemyHPLoc = VectorFunctions.findLowest(rc, enemyRobots);
    			if(lowestEnemyHPLoc != null){//attacks lowest HP enemy if in range
    				if(lowestEnemyHPLoc.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
    					rc.setIndicatorString(1, "trying to shoot");
    					if(rc.isActive()){
    						rc.attackSquare(lowestEnemyHPLoc);
    					}
    				}
    			}
    			else if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared){
    				rc.setIndicatorString(1, "trying to shoot");
    				if(rc.isActive()){
    					rc.attackSquare(closestEnemyLoc);
    				}
    			}else{
    				Direction towardClosest = rc.getLocation().directionTo(closestEnemyLoc);
    				rc.setIndicatorString(1, "trying to go closer to " + closestEnemyLoc);
    				simpleMove(towardClosest);
    			}
    		}
    		else {//if HQ is the only enemy
    			rc.setIndicatorString(1, "NEAR HQ");
    			Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
    			if(rc.isActive()&&rc.canMove(chosenDirection)){
    				rc.move(chosenDirection);
    			}
    		}
    	}else if(rc.readBroadcast(20000)!=-100){
	    	MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(20000));
    		rc.setIndicatorString(1, "Going to attack location: " + pastrLoc);
			if(path.size()==0){
				path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
			}
			//follow breadthFirst path
			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
		}else{
    		rc.setIndicatorString(1, "DOING NOTHING");
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
		if(myRole.name() == "CONSTRUCTOR"){
			tryToConstruct();
		}
		else if(myRole.name() == "COWBOY"){
			tryToGather();
		}
		else if(myRole.name() == "SOLDIER"){
			tryToShoot();
		}
		else if(myRole.name() == "DEFENDER"){
			tryToDefend();
		}
		else{
			tryToShoot();
		}

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
    
//    private static void swarmMove(int height, int width) throws GameActionException{
//        Direction chosenDirection = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
//        if(rc.isActive()){
//            if(randall.nextDouble()<0.5){//go to swarm center
//                for(int directionalOffset:directionalLooks){
//                    int forwardInt = chosenDirection.ordinal();
//                    Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
//                    if(rc.canMove(trialDir)){
//                        rc.move(trialDir);
//                        break;
//                    }
//                }
//            }else{//go wherever the wind takes you
//                Direction d = allDirections[(int)(randall.nextDouble()*8)];
//                if(rc.isActive()&&rc.canMove(d)){
//                    rc.move(d);
//                }
//            }
//        }
//    }
        
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
			if(rc.canMove(trialDir) && rc.isActive()){
				rc.move(trialDir);
				break;
			}
		}
	}
}