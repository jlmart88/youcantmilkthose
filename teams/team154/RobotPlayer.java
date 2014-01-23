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
import team154.roles.Soldier;

public class RobotPlayer{
    
    public static RobotController rc;
    public static Direction allDirections[] = Direction.values();
    public static Random randall = new Random();
    public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	public static ArrayList<MapLocation> path;
	public static int bigBoxSize = 5;
	public static int height;
	public static int width;
	public static int MAX_NOISE_DIST = 17;
	public static int DIAG_NOISE_DIST = (int)(MAX_NOISE_DIST/1.414);
	public static MapLocation enemyHQLocation;
	public static MapLocation currentPastr;
	public static boolean setInitialPath = false;
    public static MapLocation HQLocation;

	
	//constants for assigning roles
	static RobotRoles myRole = null;
    
    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        height = rc.getMapHeight();
        width = rc.getMapWidth();
        HQLocation = rc.senseHQLocation();
        enemyHQLocation = rc.senseEnemyHQLocation();
        currentPastr = rc.getLocation();
        if(rc.getType()!=RobotType.HQ && rc.getType()!=RobotType.NOISETOWER && rc.getType()!=RobotType.PASTR){
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
                }else if(rc.getType()==RobotType.NOISETOWER){
                	runTower();
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
    
    private static void tryToConstructPastr() throws GameActionException{
    	if (rc.readBroadcast(CommunicationProtocol.PASTR_LOCATION_FINISHED_CHANNEL)==1){
    		int x = rc.getRobot().getID()%3;
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
    			BasicPathing.tryToSneak(towardClosest,true,rc,directionalLooks,allDirections);
    		}
    	}
    }
    
    private static void tryToConstructTower(MapLocation[] ourPastrs) throws GameActionException{
    	MapLocation targetPastr = ourPastrs[0];
		MapLocation currentLoc = rc.getLocation();
    	rc.setIndicatorString(2, "I WILL CONSTRUCT TOWER AT " + targetPastr);
    	if(rc.isActive() && currentLoc.isAdjacentTo(targetPastr)){
    		rc.construct(RobotType.NOISETOWER);
    	}
		if(path.size()<=1&&!setInitialPath){
			path = BreadthFirst.pathTo(VectorFunctions.mldivide(currentLoc,bigBoxSize), VectorFunctions.mldivide(targetPastr,bigBoxSize), 100000);
			setInitialPath=true;
		}
		//follow breadthFirst path
			Direction towardClosest = rc.getLocation().directionTo(targetPastr);
			BasicPathing.tryToMove(towardClosest,true,rc,directionalLooks,allDirections);
    }

    private static void runTower() throws GameActionException{
    	
    	MapLocation thisLoc = rc.getLocation();
    	MapLocation eastLoc = thisLoc.add(MAX_NOISE_DIST,0);
    	MapLocation SELoc = thisLoc.add(DIAG_NOISE_DIST,DIAG_NOISE_DIST);
    	MapLocation southLoc = thisLoc.add(0,MAX_NOISE_DIST);
    	MapLocation SWLoc = thisLoc.add(-DIAG_NOISE_DIST,DIAG_NOISE_DIST);
    	MapLocation westLoc = thisLoc.add(-MAX_NOISE_DIST,0);
    	MapLocation NWLoc = thisLoc.add(-DIAG_NOISE_DIST,-DIAG_NOISE_DIST);
    	MapLocation northLoc = thisLoc.add(0,-MAX_NOISE_DIST);
    	MapLocation NELoc = thisLoc.add(DIAG_NOISE_DIST,-DIAG_NOISE_DIST);

    	rc.attackSquare(thisLoc.add(Direction.EAST));
    	while(!thisLoc.isAdjacentTo(eastLoc)){
    		while(eastLoc.x > width){
    			eastLoc=eastLoc.subtract(Direction.EAST);
    		}
    		if(rc.isActive()){
    			eastLoc = eastLoc.subtract(Direction.EAST);
    			rc.attackSquare(eastLoc);
    		}
    	}
    	while(!thisLoc.isAdjacentTo(SELoc)){
    		while(SELoc.x > width || SELoc.y > height){
    			SELoc=SELoc.subtract(Direction.SOUTH_EAST);
    		}
    		if(rc.isActive()){
    			SELoc = SELoc.subtract(Direction.SOUTH_EAST);
    			rc.attackSquare(SELoc);
    		}
    	}
    	while(!thisLoc.isAdjacentTo(southLoc)){
    		while(southLoc.y > height){
    			southLoc=southLoc.subtract(Direction.SOUTH);
    		}
    		if(rc.isActive()){
    			southLoc = southLoc.subtract(Direction.SOUTH);
    			rc.attackSquare(southLoc);
    		}
    	}
    	
    	while(!thisLoc.isAdjacentTo(SWLoc)){
    		while(SWLoc.x < 0 || SWLoc.y > height){
    			SWLoc=SWLoc.subtract(Direction.SOUTH_WEST);
    		}
    		if(rc.isActive()){
    			SWLoc = SWLoc.subtract(Direction.SOUTH_WEST);
    			rc.attackSquare(SWLoc);
    		}
    	}

    	while(!thisLoc.isAdjacentTo(westLoc)){
    		while(westLoc.x < 0){
    			westLoc=westLoc.subtract(Direction.WEST);
    		}
    		if(rc.isActive()){
    			westLoc = westLoc.subtract(Direction.WEST);
    			rc.attackSquare(westLoc);
    		}
    	}
    	
    	while(!thisLoc.isAdjacentTo(NWLoc)){
    		while(NWLoc.x < 0 || NWLoc.y < 0){
    			NWLoc=NWLoc.subtract(Direction.NORTH_WEST);
    		}
    		if(rc.isActive()){
    			NWLoc = NWLoc.subtract(Direction.NORTH_WEST);
    			rc.attackSquare(NWLoc);
    		}
    	}
    	
    	while(!thisLoc.isAdjacentTo(northLoc)){
    		while(northLoc.y < 0){
    			northLoc=northLoc.subtract(Direction.NORTH);
    		}
    		if(rc.isActive()){
    			northLoc = northLoc.subtract(Direction.NORTH);
    			rc.attackSquare(northLoc);
    		}
    	}
    	
    	while(!thisLoc.isAdjacentTo(NELoc)){
    		while(NELoc.x > width || NELoc.y < 0){
    			NELoc=NELoc.subtract(Direction.NORTH_EAST);
    		}
    		if(rc.isActive()){
    			NELoc = NELoc.subtract(Direction.NORTH_EAST);
    			rc.attackSquare(NELoc);
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
		else if(myRole.name() == "CONSTRUCTOR"){
			MapLocation[] ourPastrs = rc.sensePastrLocations(rc.getTeam());
	    	if(ourPastrs.length > 0){
	    		tryToConstructTower(ourPastrs);
	    	}
			tryToConstructPastr();
		}
		else if(myRole.name() == "COWBOY"){
			Soldier.tryToShoot(rc);
		}
		else if(myRole.name() == "SOLDIER"){
			Soldier.tryToShoot(rc);
		}
		else if(myRole.name() == "DEFENDER"){
			Soldier.tryToDefend(rc);
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

    
	public static void simpleMove(Direction chosenDirection) throws GameActionException{
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
//	
	public static void simpleMoveAgainst(Direction chosenDirection) throws GameActionException{
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