package team154;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import battlecode.common.*;

public class MapAnalyzer {
	
	public static final char NORMAL_TILE = '-';
	public static final char VOID_TILE = '#';
	public static final char OFF_MAP_TILE = '^';
	public static final char ROAD_TILE = '=';
	public static final int NUM_IDEAL_PASTRS = 5;
	
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
	public static MapLocation[] findIdealPastrLocations(char[][] terrainMap, double[][] cowGrowthMap, MapLocation hqLocation){
		MapLocation idealLocations[] = new MapLocation[NUM_IDEAL_PASTRS];
		HashMap<Integer, MapLocation> rankedMap = 
				new HashMap<Integer,MapLocation>(); //create space for a map where
													//each location has a rank (value)
		
		for (int rowNum=0; rowNum<terrainMap.length; rowNum++){
			for (int colNum=0; colNum<terrainMap[0].length; colNum++){
				
				if (terrainMap[rowNum][colNum]==NORMAL_TILE||terrainMap[rowNum][colNum]==ROAD_TILE){
					
					//determine the number of ways to get to the location in a single move
					MapLocation[] possibleEntryways = MapLocation.getAllMapLocationsWithinRadiusSq(new MapLocation(colNum,rowNum), 2);
					
					int numEntryways = 0;
					for (MapLocation possibleEntryway:possibleEntryways){
						
						
						if(possibleEntryway.x>=0 && possibleEntryway.y>=0 && 
								possibleEntryway.x<terrainMap[0].length && possibleEntryway.y<terrainMap.length){
							char terrain = terrainMap[possibleEntryway.y][possibleEntryway.x];
								if ((terrain==NORMAL_TILE||terrain==ROAD_TILE)&&!(possibleEntryway.x==colNum&&possibleEntryway.y==rowNum)){
									numEntryways++;
									}
						}
						
					}
					
					
					//determine the cow growth rate at the location
					double cowGrowthRate = cowGrowthMap[rowNum][colNum];
					
					//determine the distance to the HQ
					int proximityToHQ = hqLocation.distanceSquaredTo(new MapLocation(colNum,rowNum));
					
					//determine the number of locations from which the location can be attacked
					MapLocation[] locationsInAttackRange = 
							MapLocation.getAllMapLocationsWithinRadiusSq(
									new MapLocation(colNum,rowNum),RobotType.SOLDIER.attackRadiusMaxSquared);
					int numAttackLocations = 0;
					
					for (MapLocation location: locationsInAttackRange){
						if(location.x>=0 && location.y>=0 && 
								location.x<terrainMap[0].length && location.y<terrainMap.length){
							char terrain = terrainMap[location.y][location.x];
							if (terrain==NORMAL_TILE||terrain==ROAD_TILE){
								numAttackLocations++;
							}
						}
						
					}
					
					//weight these factors into a ranking:
					int entrywayWeight = 10000;
					int cowGrowthWeight = 1500;
					int proximityWeight = 1000;
					int attackWeight = 10000;
					
					rankedMap.put((int) (
							entrywayWeight/(numEntryways+1)+
							cowGrowthWeight*cowGrowthRate+
							proximityWeight/(proximityToHQ+1)+
							attackWeight/(numAttackLocations+1)),
							new MapLocation(colNum,rowNum));
					
						
				}else{
					//the tile can't be built on, so don't put it in the rankings
				}
			}
		}
		
		//find the top pastr locations
		List<Integer> rankings =  new ArrayList<Integer>(rankedMap.keySet());
		Collections.sort(rankings, new Comparator<Integer>() {
			@Override public int compare(Integer one, Integer two) {
				return two.compareTo(one);
			}
		});
		
		for (int i=0; i<NUM_IDEAL_PASTRS; i++){
			int bestRanking = rankings.get(i);
			MapLocation bestLocation = rankedMap.get(bestRanking);
			System.out.println("BEST RANKING: "+bestRanking+ " LOCATION: "+bestLocation);
			idealLocations[i]=bestLocation;
		}
		
		return idealLocations;
		
		
	}
	
	/**
	 * Returns the locations surrounding center in a square
	 * as MapLocations
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
	 * @param center MapLocation representing the center of the square
	 * @param distanceAway int representing the distance within to return points
	 * @return MapLocation[]
	 */
	private static MapLocation[] neighborLocations(MapLocation center, int distanceAway){
		int numOfNeighbors = (((distanceAway*2)+1)*((distanceAway*2)+1))-1;
		MapLocation out[] = new MapLocation[numOfNeighbors];
		int index=0;
		for (int i=-distanceAway; i<=distanceAway; i++){
			for (int j=-distanceAway; j<=distanceAway; j++) {
				if (i!=0&&j!=0){
					out[index] = new MapLocation(center.x+i,center.y+j);
				}
			}
		}
		return out;
		
	}
	
	public static void printIdealPastrLocations(char[][] terrainMap, MapLocation[] idealLocations) {
		System.out.println("***********PASTR LOCATIONS************");
		ArrayList<MapLocation> idealLocationsArray = new ArrayList<MapLocation>(Arrays.asList(idealLocations));
		for(int y=0; y<terrainMap.length; y++){
            for(int x=0; x<terrainMap[0].length; x++){
            	if (idealLocationsArray.contains(new MapLocation(x,y))){
            		System.out.print("X");
            	}
            	else{
            		System.out.print(terrainMap[y][x]);
            	}
            }
            System.out.println();
		}
	}

}
