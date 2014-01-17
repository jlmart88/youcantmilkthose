package team154;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

import team154.movement.BasicPathing;
import team154.movement.BreadthFirst;
import team154.movement.VectorFunctions;
import team154.roles.Cowboy;
import team154.roles.Headquarters;
import team154.roles.RobotRoles;

public class RobotPlayer{
    
    public static RobotController rc;
    public static Direction allDirections[] = Direction.values();
    public static Random randall = new Random();
    public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	public static ArrayList<MapLocation> path;
	public static int bigBoxSize = 5;
	public static int height;
	public static int width;
	public static MapLocation enemyHQLocation;
	
	static MapLocation currentPastr;
	static boolean setInitialPath = false;
	
	//constants for assigning roles
	static RobotRoles myRole = null;
    
    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        height = rc.getMapHeight();
        width = rc.getMapWidth();
        enemyHQLocation = rc.senseEnemyHQLocation();
        currentPastr = rc.getLocation();
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
               e.printStackTrace();
            }
        }
    }


    public static boolean closeEnough(MapLocation loc1, MapLocation loc2, int dist){
    	if(Math.abs(loc1.x - loc2.x) + Math.abs(loc1.y - loc2.y) <= dist){
    		return true;
    	}
    	return false;
    }
    
    private static void tryToConstruct() throws GameActionException{
    	if (rc.readBroadcast(CommunicationProtocol.PASTR_LOCATION_FINISHED_CHANNEL)==1){
    		int x = rc.getRobot().getID()%5;
    		MapLocation currentLoc = rc.getLocation();
    		MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(CommunicationProtocol.PASTR_LOCATION_CHANNEL_MIN+x));
    		rc.setIndicatorString(2, "I WILL CONSTRUCT AT " + pastrLoc);
    		//there are no enemies, so build a tower
    		if(rc.sensePastrLocations(rc.getTeam()).length<10){
    			if (senseCowsAtRange(rc.getLocation()) > 3500 || currentLoc.equals(pastrLoc)){
    				if(rc.isActive()){
    					rc.construct(RobotType.PASTR);
    				}
    			}
    		}
    		if(path.size()<=1&&!setInitialPath){
    			path = BreadthFirst.pathTo(VectorFunctions.mldivide(currentLoc,bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    			setInitialPath=true;
    		}
    		//follow breadthFirst path
    		if(!closeEnough(currentLoc,pastrLoc,5)&&path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
    		}
    		else{
    			Direction towardClosest = rc.getLocation().directionTo(pastrLoc);
    			BasicPathing.tryToMove(towardClosest,true,rc,directionalLooks,allDirections);
    		}
    	}
    }
    
    private static void moveTowards(MapLocation enemyLoc) throws GameActionException{
			//System.out.println("going closer");
			//    					rc.broadcast(5000,VectorFunctions.locToInt(closestEnemyLoc));
			Direction towardClosest = rc.getLocation().directionTo(enemyLoc);
			rc.setIndicatorString(1, "trying to go closer to " + enemyLoc);
			BasicPathing.tryToMove(towardClosest,true,rc,directionalLooks,allDirections);
    }
    
    private static void attackPastrs(Robot[] alliedRobots) throws GameActionException{
		rc.setIndicatorString(0, "There are pastrs to attack");
		rc.setIndicatorString(1, "Going to attack location: " + currentPastr);
		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());
		boolean inList = false;
		
		for (MapLocation location:pastrLocs){
			if (location.equals(currentPastr)){
				inList=true;
			}
		}
		if (!inList){
			MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(20000));
			currentPastr = pastrLoc;
			setInitialPath = false;
			inList = true;
		}
		if (inList&&!setInitialPath&&alliedRobots.length>3&&closeEnough(rc.getLocation(),enemyHQLocation,2)){
			rc.setIndicatorString(2, "Setting path to: "+currentPastr);
			path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(currentPastr,bigBoxSize), 100000);
			rc.setIndicatorString(2, "Got path");
			setInitialPath = true;
		}
		else if(inList&&!setInitialPath&&!closeEnough(rc.getLocation(),enemyHQLocation,2)){
			rc.setIndicatorString(2, "Setting path to: "+currentPastr);
			path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(currentPastr,bigBoxSize), 100000);
			rc.setIndicatorString(2, "Got path");
			setInitialPath = true;
//			MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
//			rc.setIndicatorString(2, "Moving towards cluster");
//			MapLocation toCluster = VectorFunctions.meanLocation(alliedRobotLocations);
//			if(toCluster != null)
//				BasicPathing.tryToMove(rc.getLocation().directionTo(toCluster), true, rc, directionalLooks, allDirections);
		}
		if(path.size()<=1&&setInitialPath){
			BasicPathing.tryToMove(rc.getLocation().directionTo(currentPastr),true, rc, directionalLooks, allDirections);
		}
		//follow breadthFirst path
		else if(path.size()>1){
			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			
		}
    }
    public static void tryToShoot() throws GameActionException{
    	Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
    	Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,35,rc.getTeam());
    	if(enemyRobots.length>0){//if there are enemies
    		if(!(enemyRobots.length==1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){
    			rc.setIndicatorString(0, "There are enemies");
    			int locationSize = enemyRobots.length;
    			MapLocation[] robotLocations = new MapLocation[locationSize];
    			for(int i=0;i<locationSize;i++){
    				Robot anEnemy = enemyRobots[i];
    				RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
    				robotLocations[i] = anEnemyInfo.location;
    			}
    			MapLocation closestEnemyLoc = VectorFunctions.findClosest(robotLocations, rc.getLocation());
    			MapLocation lowestHPEnemyLoc = VectorFunctions.findLowest(rc, enemyRobots);
    			rc.setIndicatorString(2,"Our robots #: "+alliedRobots.length + " Enemy robots #:" + enemyRobots.length);
    			if(alliedRobots.length+1 >= enemyRobots.length){
    				if(lowestHPEnemyLoc!=null){
    					if(lowestHPEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared && lowestHPEnemyLoc!=enemyHQLocation){// attacks lowest HP enemy if in range
    						rc.setIndicatorString(1, "trying to shoot lowest");
    						if(rc.isActive()){
    							rc.attackSquare(lowestHPEnemyLoc);
    						}
    					}
    				}else if(closestEnemyLoc!=null){
    					if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared && closestEnemyLoc!=enemyHQLocation){// attacks the closest enemy if in range
    						rc.setIndicatorString(1, "trying to shoot closest");
    						if(rc.isActive()){
    							rc.attackSquare(closestEnemyLoc);
    						}
    					}
    					
    				}else{
						moveTowards(closestEnemyLoc);
    				}
    			}
    			else{
    				rc.setIndicatorString(1, "Retreating");
    				BasicPathing.tryToMove(rc.getLocation().directionTo(closestEnemyLoc).opposite(),true,rc,directionalLooks,allDirections);
    			}
    		}
    		else{
    			attackPastrs(alliedRobots);
    		}
    	}
    else if(rc.readBroadcast(20000)!=-100){
    	attackPastrs(alliedRobots);
    	}else if (rc.sensePastrLocations(rc.getTeam()).length>0){
    		rc.setIndicatorString(0, "There are pastrs to defend");
    		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam());
    		//choose one of our pastrs and move toward it
    		int x = rc.getRobot().getID()%pastrLocs.length;
    		MapLocation pastrLoc = pastrLocs[x];
    		if (!setInitialPath){
				path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    			setInitialPath = true;
			}
    		if(path.size()<=1&&setInitialPath){
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLoc),true, rc, directionalLooks, allDirections);
    		}
    		//follow breadthFirst path
			else if(path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			}
    	}
    	else{
    		rc.setIndicatorString(0, "DOING NOTHING");
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
		else if(myRole.name() == "CONSTRUCTOR"){
			tryToConstruct();
		}
		else if(myRole.name() == "COWBOY"){
			Cowboy.tryToGather(rc);
		}
		else if(myRole.name() == "SOLDIER"){
			tryToShoot();
		}
		else if(myRole.name() == "DEFENDER"){
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
				rc.setIndicatorString(1,"MOVING TO" + trialDir);
				rc.move(trialDir);
				break;
			}
		}
	}
	
	private static void simpleMoveAgainst(Direction chosenDirection) throws GameActionException{
		for(int directionalOffset:directionalLooks){
			int forwardInt = chosenDirection.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+12)%8];
			if(rc.canMove(trialDir) && rc.isActive()){
				rc.move(trialDir);
				break;
			}
		}
	}

}