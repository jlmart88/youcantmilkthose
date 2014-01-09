package kevinsTeam;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer{
    
    public static RobotController rc;
    static Direction allDirections[] = Direction.values();
    static Random randall = new Random();
    static int directionalLooks[] = new int[]{0,1,-1,2,-2};
    static boolean mapCreated = false;
    
    public static void run(RobotController rcin){
        rc = rcin;
        randall.setSeed(rc.getRobot().getID());
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        while(true){
            try{
                if(rc.getType()==RobotType.HQ){//if I'm a headquarters
                    runHeadquarters(height,width);
                }else if(rc.getType()==RobotType.SOLDIER){
                    runSoldier(height, width);
                }
                rc.yield();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void runSoldier(int height, int width) throws GameActionException {
        tryToShoot();
        //movement
        Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
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
        swarmMove(height, width);
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

    private static void tryToShoot() throws GameActionException {
        //shooting
        Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
        if(enemyRobots.length>0){//if there are enemies
            Robot anEnemy = enemyRobots[0];
            RobotInfo anEnemyInfo;
            anEnemyInfo = rc.senseRobotInfo(anEnemy);
            if(anEnemyInfo.type == RobotType.HQ && enemyRobots.length>1){
                anEnemy = enemyRobots[1];
                anEnemyInfo = rc.senseRobotInfo(anEnemy);
                if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
                    if(rc.isActive()){
                        rc.attackSquare(anEnemyInfo.location);
                    }
                }
            }
            else if(anEnemyInfo.type != RobotType.HQ){
                if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
                    if(rc.isActive()){
                        rc.attackSquare(anEnemyInfo.location);
                    }
                }
            }
        }else{//there are no enemies, so build a tower
            if(randall.nextDouble()<1 && rc.sensePastrLocations(rc.getTeam()).length<10){
                if (senseCowsAtRange(rc.getLocation()) > 2500){
                    if(rc.isActive()){
                        rc.construct(RobotType.PASTR);
//                        System.out.println((senseCowsAtRange(rc.getLocation())));
                    }
                }
            }
        }
    }

    
    private static void runHeadquarters(int height, int width) throws GameActionException {
        char[][] map = new char[width][height];
        Direction spawnDir = Direction.NORTH;
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