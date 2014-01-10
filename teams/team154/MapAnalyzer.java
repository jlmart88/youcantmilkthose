package team154;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import battlecode.common.*;

public class MapAnalyzer {
	
	public final char terrainMap[][];
	public final double cowGrowthMap[][];
	public final MapLocation hqLocation;
	public static final char NORMAL_TILE = '-';
	public static final char VOID_TILE = '#';
	public static final char OFF_MAP_TILE = '^';
	public static final char ROAD_TILE = '=';
	public static final int NUM_IDEAL_PASTRS = 5;
	
	public MapAnalyzer(char[][] terrainMap, double[][] cowGrowthMap, MapLocation hqLocation) {
		this.terrainMap = terrainMap;
		this.cowGrowthMap = cowGrowthMap;
		this.hqLocation = hqLocation;
	}
	
	/** Returns NUM_IDEAL_PASTRS number of locations where building a pastr is ideal
	 * 
	 * Checks the following parameters:
	 * -numEntryways = number of ways to get to the point in one move (0-8)
	 * -cowGrowthRate = rate that cows grow at at that spot (0-.5)
	 * -proximityToHQ = how far the point is from the HQ (0-100)
	 * -numAttackLocations = number of locations surrounding point from which point can be attacked (0-36 [w/ r^2=10])
	 *
	 * @return MapLocation[] array of MapLocations to build a pastr
	 */
	public MapLocation[] findIdealPastrLocations(){
		MapLocation optimalLocations[] = new MapLocation[NUM_IDEAL_PASTRS];
		HashMap<Integer, MapLocation> rankedMap = 
				new HashMap<Integer,MapLocation>(); //create space for a map where
													//each location has a rank (value)
		
		int rowNum = -1;
		for (char[] row:terrainMap){
			rowNum++;
			int colNum = -1;
			for (char tile:row){
				colNum++;
				if (tile==NORMAL_TILE||tile==ROAD_TILE){
					
					//determine the number of ways to get to the location in a single move
					Point[] possibleEntryways = neighborLocations(new Point(rowNum,colNum),1);
					int numEntryways = 0;
					for (Point possibleEntryway:possibleEntryways){
						char terrain = terrainMap[possibleEntryway.y][possibleEntryway.x];
						if (terrain==NORMAL_TILE||terrain==ROAD_TILE){
							numEntryways++;
						}
					}
					
					//determine the cow growth rate at the location
					double cowGrowthRate = cowGrowthMap[rowNum][colNum];
					
					//determine the distance to the HQ
					int proximityToHQ = hqLocation.distanceSquaredTo(new MapLocation(rowNum,colNum));
					
					//determine the number of locations from which the location can be attacked
					MapLocation[] locationsInAttackRange = 
							MapLocation.getAllMapLocationsWithinRadiusSq(
									new MapLocation(rowNum, colNum),RobotType.SOLDIER.attackRadiusMaxSquared);
					int numAttackLocations = 0;
					for (MapLocation location: locationsInAttackRange){
						char terrain = terrainMap[location.y][location.x];
						if (terrain==NORMAL_TILE||terrain==ROAD_TILE){
							numAttackLocations++;
						}
					}
					
					//weight these factors into a ranking:
					int entrywayWeight = 10000;
					int cowGrowthWeight = 2000;
					int proximityWeight = 10000;
					int attackWeight = 10000;
					
					rankedMap.put((int) ((int)
							entrywayWeight/(numEntryways+1)+
							cowGrowthWeight*cowGrowthRate+
							proximityWeight/(proximityToHQ+1)+
							attackWeight/(numAttackLocations+1)),
							new MapLocation(rowNum,colNum));
					
						
				}else{
					//the tile can't be built on, so don't put it in the rankings
				}
			}
		}
		
		//find the top pastr locations
		List<Integer> rankings = (List<Integer>) rankedMap.keySet();
		Collections.sort(rankings);
		for (int i=0; i<NUM_IDEAL_PASTRS; i++){
			int bestRanking = rankings.get(i);
			optimalLocations[i]=rankedMap.get(bestRanking);
		}
		
		return optimalLocations;
		
		
	}
	
	/**
	 * Returns the locations surrounding center in a square
	 * as Points
	 * 
	 * 
	 * Ex: distanceAway=1 returns the following x points:
	 * x x x
	 * x c x
	 * x x x
	 * 
	 * where c is the center.
	 * 
	 * distanceAway=2 returns:
	 * x x x x x
	 * x x x x x 
	 * x x c x x 
	 * x x x x x 
	 * x x x x x 
	 * 
	 * @param center Point representing the center of the square
	 * @param distanceAway int representing the distance within to return points
	 * @return Point[]
	 */
	private Point[] neighborLocations(Point center, int distanceAway){
		int numOfNeighbors = (((distanceAway*2)+1)*((distanceAway*2)+1))-1;
		Point out[] = new Point[numOfNeighbors];
		int index=0;
		for (int i=-distanceAway; i<=distanceAway; i++){
			for (int j=-distanceAway; j<=distanceAway; j++) {
				if (i!=0&&j!=0){
					out[index] = new Point(center.x+i,center.y+j);
				}
			}
		}
		return out;
		
	}
	

}
