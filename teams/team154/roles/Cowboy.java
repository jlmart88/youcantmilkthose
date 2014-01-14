package team154.roles;

import java.util.ArrayList;

import team154.RobotPlayer;
import team154.movement.BasicPathing;
import team154.movement.BreadthFirst;
import team154.movement.VectorFunctions;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Cowboy {
	
	static MapLocation pastrLoc = null;
	
	@SuppressWarnings("unused")
	public static void tryToGather(RobotController rc) throws GameActionException{
    	MapLocation[] pastrLocs = rc.sensePastrLocations(rc.getTeam());
    	
    	if(pastrLocs.length>0){
    		int x = rc.getRobot().getID()%pastrLocs.length;
    		if(RobotPlayer.path.size()==0){
    			pastrLoc = pastrLocs[x];
    		
    		RobotPlayer.path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),RobotPlayer.bigBoxSize), 
    				VectorFunctions.mldivide(pastrLoc,RobotPlayer.bigBoxSize), 100000);
    		}
    		//follow breadthFirst path
    		if(!RobotPlayer.closeEnough(rc.getLocation(),pastrLoc)){
    			Direction bdir = BreadthFirst.getNextDirection(RobotPlayer.path, RobotPlayer.bigBoxSize);
    			BasicPathing.tryToMove(bdir, true, rc, RobotPlayer.directionalLooks, RobotPlayer.allDirections);
    		}
    		else{ //run a farming pattern
    			Direction dirToPastr = rc.getLocation().directionTo(pastrLoc);
    			Direction dirOrthogonal;
    			if (RobotPlayer.randall.nextDouble()<.5){
    				dirOrthogonal = dirToPastr.rotateRight();
    			} else{
    				dirOrthogonal = dirToPastr.rotateLeft().rotateLeft();
    				
    			}
    			BasicPathing.tryToMove(dirOrthogonal, true, rc, RobotPlayer.directionalLooks, RobotPlayer.allDirections);
    		}
    	}
    }
	
	private static void generateFarmingPath(){
		
	}

}
