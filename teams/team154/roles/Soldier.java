package team154.roles;

import team154.CommunicationProtocol;
import team154.RobotPlayer;
import team154.movement.BasicPathing;
import team154.movement.BreadthFirst;
import team154.movement.VectorFunctions;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Soldier {
	
    public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
    public static Direction allDirections[] = Direction.values();
    public static int bigBoxSize = 5;

    private static void moveTowards(RobotController rc, MapLocation enemyLoc) throws GameActionException{
			//System.out.println("going closer");
			//    					rc.broadcast(5000,VectorFunctions.locToInt(closestEnemyLoc));
			Direction towardClosest = rc.getLocation().directionTo(enemyLoc);
			rc.setIndicatorString(1, "trying to go closer to " + enemyLoc);
			BasicPathing.tryToMove(towardClosest,true,rc,directionalLooks,allDirections);
    }
    
    private static void attackPastrs(RobotController rc, Robot[] alliedRobots) throws GameActionException{
		rc.setIndicatorString(0, "There are pastrs to attack");
		rc.setIndicatorString(1, "Going to attack location: " + RobotPlayer.currentPastr);
		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam().opponent());
		boolean inList = false;
		
		for (MapLocation location:pastrLocs){
			if (location.equals(RobotPlayer.currentPastr)){
				inList=true;
			}
		}
		if (!inList){
			MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(20000));
			RobotPlayer.currentPastr = pastrLoc;
			RobotPlayer.setInitialPath = false;
			inList = true;
		}
		if (inList&&!RobotPlayer.setInitialPath&&RobotPlayer.closeEnough(RobotPlayer.currentPastr,RobotPlayer.enemyHQLocation,2)){ // if closest pastr is near their HQ
			rc.broadcast(14000, 100); // switch to build tactic
		}
		else if(inList&&!RobotPlayer.setInitialPath&&!RobotPlayer.closeEnough(RobotPlayer.currentPastr,RobotPlayer.enemyHQLocation,2)){
			rc.broadcast(14000,0);
			rc.setIndicatorString(2, "Setting path to: "+RobotPlayer.currentPastr);
			RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(RobotPlayer.currentPastr,bigBoxSize), 100000);
			rc.setIndicatorString(2, "Got path");
			RobotPlayer.setInitialPath = true;
//			MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
//			rc.setIndicatorString(2, "Moving towards cluster");
//			MapLocation toCluster = VectorFunctions.meanLocation(alliedRobotLocations);
//			if(toCluster != null)
//				BasicPathing.tryToMove(rc.getLocation().directionTo(toCluster), true, rc, directionalLooks, allDirections);
		}
		if(RobotPlayer.path.size()<=1&&RobotPlayer.setInitialPath){
			BasicPathing.tryToMove(rc.getLocation().directionTo(RobotPlayer.currentPastr),true, rc, directionalLooks, allDirections);
		}
		//follow breadthFirst path
		else if(RobotPlayer.path.size()>1){
			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			
		}
    }

    public static void combat(RobotController rc, Robot[] enemyRobots, Robot[] alliedRobots, boolean defend) throws GameActionException{
    	if(!(enemyRobots.length==1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){// if the enemy isn't a HQ
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
//			if(lowestHPEnemyLoc!=null){
//				rc.setIndicatorString(2, "LOCATION: " + lowestHPEnemyLoc);
//			}
			rc.setIndicatorString(2,"Our robots #: "+alliedRobots.length + " Enemy robots #:" + enemyRobots.length);
			if(alliedRobots.length+1 >= enemyRobots.length){
				if(lowestHPEnemyLoc!=null){
					if(lowestHPEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared && lowestHPEnemyLoc!=RobotPlayer.enemyHQLocation){// attacks lowest HP enemy if in range
						rc.setIndicatorString(1, "trying to shoot lowest");
						if(rc.isActive()){
							rc.attackSquare(lowestHPEnemyLoc);
						}
					}
    				else if(closestEnemyLoc!=null){
    					if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared && closestEnemyLoc!=RobotPlayer.enemyHQLocation){// attacks the closest enemy if in range
    						rc.setIndicatorString(1, "trying to shoot closest");
    						if(rc.isActive()){
    							rc.attackSquare(closestEnemyLoc);
    						}
    					}
    					else if(!defend){
    						moveTowards(rc,closestEnemyLoc);
    					}
    				}
    				else if(!defend){
						moveTowards(rc,closestEnemyLoc);
					}
				}
				else if(closestEnemyLoc!=null){
					if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared && closestEnemyLoc!=RobotPlayer.enemyHQLocation){// attacks the closest enemy if in range
						rc.setIndicatorString(1, "trying to shoot closest");
						if(rc.isActive()){
							rc.attackSquare(closestEnemyLoc);
						}
					}
					else if(!defend){
						moveTowards(rc,closestEnemyLoc);
					}
				}
				else if(!defend){
					moveTowards(rc,closestEnemyLoc);
				}
			}
			else if(closestEnemyLoc!=null){
				if(RobotPlayer.closeEnough(closestEnemyLoc, rc.getLocation(), rc.getType().attackRadiusMaxSquared)){//if closest enemy is within attack range
					rc.setIndicatorString(1, "Retreating");
					RobotPlayer.simpleMoveAgainst(rc.getLocation().directionTo(closestEnemyLoc));
//					BasicPathing.tryToMove(rc.getLocation().directionTo(closestEnemyLoc).opposite(),true,rc,directionalLooks,allDirections);
				}
				else{//regroup with allies
					MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc,false);
					MapLocation alliedRobotCenter = VectorFunctions.meanLocation(alliedRobotLocations);
					Direction towardAllies = rc.getLocation().directionTo(alliedRobotCenter);
					BasicPathing.tryToMove(towardAllies, true, rc, directionalLooks, allDirections);
				}
			}
			else{
				MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc,false);
				MapLocation alliedRobotCenter = VectorFunctions.meanLocation(alliedRobotLocations);
				Direction towardAllies = rc.getLocation().directionTo(alliedRobotCenter);
				BasicPathing.tryToMove(towardAllies, true, rc, directionalLooks, allDirections);
			}
		}
    }
	
    public static void tryToShoot(RobotController rc) throws GameActionException{
    	Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
    	Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,35,rc.getTeam());
    	if(enemyRobots.length>0){//if there are enemies
    		combat(rc, enemyRobots, alliedRobots, false);
    	}
    	else if(rc.readBroadcast(20000)!=-100 && !RobotPlayer.closeEnough(VectorFunctions.intToLoc(rc.readBroadcast(20000)), RobotPlayer.enemyHQLocation, 3)){
    		attackPastrs(rc,alliedRobots);
    	}
    	else if (rc.sensePastrLocations(rc.getTeam()).length>0){ //move to defend pastrs
    		rc.setIndicatorString(0, "There are pastrs to defend");
    		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam());
    		//choose one of our pastrs and move toward it
    		int x = rc.getRobot().getID()%pastrLocs.length;
    		MapLocation pastrLoc = pastrLocs[x];
    		if(rc.getLocation().isAdjacentTo(pastrLoc)){
    			rc.yield();
    		}
    		else if (!RobotPlayer.setInitialPath){
    			RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    			RobotPlayer.setInitialPath = true;
			}
    		else if(RobotPlayer.path.size()<=1&&RobotPlayer.setInitialPath){
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLoc),true, rc, directionalLooks, allDirections);
    		}
    		//follow breadthFirst path
			else if(RobotPlayer.path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			}
    	}
    	else if (rc.readBroadcast(15000)!=0){
    		MapLocation prePastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(15000));
    		if (!RobotPlayer.setInitialPath){
    			RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(prePastrLoc,bigBoxSize), 100000);
    			RobotPlayer.setInitialPath = true;
    		}
    		if(RobotPlayer.path.size()<=1&&RobotPlayer.setInitialPath){
    			BasicPathing.tryToMove(rc.getLocation().directionTo(prePastrLoc),true, rc, directionalLooks, allDirections);
    		}
    		//follow breadthFirst path
    		else if(RobotPlayer.path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
    		}
    	}
    	else{
    		rc.setIndicatorString(0, "DOING NOTHING");
    		Direction d = allDirections[(int)(RobotPlayer.randall.nextDouble()*8)];
    		if(rc.isActive()&&rc.canMove(d)){
    			rc.move(d);
    		}
    	}
    }
    
    public static void tryToDefend(RobotController rc) throws GameActionException{
    	Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
    	Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,35,rc.getTeam());
    	int pastrNum = rc.sensePastrLocations(rc.getTeam()).length;
    	int pastrNumOpp = rc.sensePastrLocations(rc.getTeam().opponent()).length;
    	if(enemyRobots.length>0){//if there are enemies
    		if(rc.senseRobotInfo(enemyRobots[0]).type != RobotType.HQ){
    			rc.broadcast(12000, VectorFunctions.locToInt(rc.senseRobotInfo(enemyRobots[0]).location));
    			rc.broadcast(12001, 0);
    		}
    		combat(rc, enemyRobots, alliedRobots, false);
    		enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
    		if(enemyRobots.length==0){
    			rc.broadcast(12001, -100);
    		}
    	}
    	else if(rc.readBroadcast(12000)!=0 && rc.readBroadcast(12001)!=-100){
    		rc.setIndicatorString(1, "Attacking robots");
    		BasicPathing.tryToMove(rc.getLocation().directionTo(VectorFunctions.intToLoc(rc.readBroadcast(12000))), true, rc, directionalLooks, allDirections);
    	}
    	else if(pastrNumOpp > pastrNum){
    		attackPastrs(rc,alliedRobots);
    	}
    	else if (rc.sensePastrLocations(rc.getTeam()).length>0){ //move to defend pastrs
    		rc.setIndicatorString(0, "There are pastrs to defend");
    		MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam());
    		//choose one of our pastrs and move toward it
    		int x = rc.getRobot().getID()%pastrLocs.length;
    		MapLocation pastrLoc = pastrLocs[x];
    		if(rc.getLocation().isAdjacentTo(pastrLoc)){
    			rc.yield();
    		}
    		else if (!RobotPlayer.setInitialPath){
    			RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    			RobotPlayer.setInitialPath = true;
			}
    		else if(RobotPlayer.path.size()<=1&&RobotPlayer.setInitialPath){
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLoc),true, rc, directionalLooks, allDirections);
    		}
    		//follow breadthFirst path
    		else if(RobotPlayer.path.size()>1 && RobotPlayer.closeEnough(rc.getLocation(), pastrLoc, 5)){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
    			BasicPathing.tryToSneak(bdir, true, rc, directionalLooks, allDirections);
    		}
			else if(RobotPlayer.path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			}
    	}
        else if(rc.readBroadcast(CommunicationProtocol.PASTR_LOCATION_FINISHED_CHANNEL)==1){
    		MapLocation pastrLoc = VectorFunctions.intToLoc(rc.readBroadcast(CommunicationProtocol.PASTR_LOCATION_CHANNEL_MIN));
    		if (!RobotPlayer.setInitialPath){
    			RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(pastrLoc,bigBoxSize), 100000);
    			RobotPlayer.setInitialPath = true;
			}
    		else if(RobotPlayer.path.size()<=1&&RobotPlayer.setInitialPath){
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLoc),true, rc, directionalLooks, allDirections);
    		}
    		//follow breadthFirst path
			else if(RobotPlayer.path.size()>1){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			}
        }
        else{
        	rc.setIndicatorString(0, "DOING NOTHING");
        	Direction d = allDirections[(int)(RobotPlayer.randall.nextDouble()*8)];
        	if(rc.isActive()&&rc.canMove(d)){
        		rc.move(d);
        	}
        }
    }
}
