package sail.g4;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    
    public final double WINDSPEED = 50.0;
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    int numPlayers = 0;
    Point wind_direction;
    double wind_angle;
    ArrayList<Point> currentQueue = new ArrayList<Point>();
    boolean inStraightMode = false;
    SailingHelper sHelper;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        this.wind_direction = wind_direction;
        this.wind_angle = Math.atan2(wind_direction.y, wind_direction.x);
        gen = new Random(seed);
        initial = new Point(5.0+2*Math.cos(wind_angle), 5.0+2*Math.sin(wind_angle));//new Point(gen.nextDouble()*10, gen.nextDouble()*10);

        // double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.sHelper = new SailingHelper(targets, id, initial, wind_direction, wind_angle);
        this.numPlayers = group_locations.size();
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        // testing timeouts... 
        // try {
        //     TimeUnit.MILLISECONDS.sleep(1);
        // } catch(Exception ex) {
        //     ;
        // }
        // just for first turn
        //System.out.println(Simulator.getSpeed(new Point(.5, .5), new Point(-.5, -.5)));
        Point currentLoc = group_locations.get(id);
        //System.out.println(currentLoc.x + ", " + currentLoc.y);
        Point goalLoc;
        Point prevTarget;
        ArrayList<Integer> availableTargetIndices = new ArrayList<Integer>();
        // short-circuiting is great
        if(visited_set != null && visited_set.get(id).size() == targets.size()) {
            //this is if finished visiting all
            goalLoc = initial;
        } else {
            for(int i = 0; i < targets.size(); i++) {
                // short-circuiting remains great
                if (visited_set == null || !visited_set.get(id).contains(i)) {
                    availableTargetIndices.add(i);
                }
            }
            
            // either A:
            if(availableTargetIndices.size()>7*targets.size()/10)
                goalLoc = sHelper.getClosestTargetByTime(availableTargetIndices, currentLoc);
            // \end A.
            else
                goalLoc = sHelper.getHeuristicDistance(availableTargetIndices,currentLoc,numPlayers,group_locations);


            // or B:
            // int k = 6;
            // if (!currentQueue.isEmpty()) {
            //     // if the first in line has not been seen yet
            //     if (availableTargetIndices.contains(targets.indexOf(currentQueue.get(0)))) {
            //         goalLoc = currentQueue.get(0);
            //     } else {
            //         // if the first in queue has been seen already
            //         currentQueue.remove(0);
            //         if (!currentQueue.isEmpty()) {
            //             goalLoc = currentQueue.get(0);
            //         } else {
            //             currentQueue = sHelper.getkOptimalTargets(availableTargetIndices, currentLoc, k);
            //             goalLoc = currentQueue.get(0);
            //         }
            //     }
            // } else {
            //     // System.out.println("AVAILABLE TARGET INDICES:");
            //     // for (Integer i : availableTargetIndices) {
            //     //     System.out.print(i + " ");
            //     // }
            //     //System.out.println();
            //     currentQueue = sHelper.getkOptimalTargets(availableTargetIndices, currentLoc, k);
            //     goalLoc = currentQueue.get(0);
            //     System.out.println("new target is " + goalLoc.x + ", " + goalLoc.y);
            // }
            // \end B.

        }
        double relAngleWouldNeedToGoIn = sHelper.getRelAngle(currentLoc, goalLoc);
        // double actualAngleWouldNeedToGoIn = getAbsoluteAngle(currentLoc, goalLoc);
        // double dx = dt * getSpeed(getBestTheta(actualAngleWouldNeedToGoIn)-wind_angle+Math.PI) * Math.cos(getBestAbsoluteAngle(currentLoc, goalLoc));
        // double dy = dt * getSpeed(getBestTheta(actualAngleWouldNeedToGoIn)-wind_angle+Math.PI) * Math.sin(getBestAbsoluteAngle(currentLoc, goalLoc));
        // Point newPointCurvingWouldGive = Point.sum(currentLoc, new Point(dx, dy));
        // if (shouldCurve(relAngleWouldNeedToGoIn) && !isInsideBox(newPointCurvingWouldGive)) {
        //     System.out.println("prevented colliding with border. was at "+ currentLoc.x + ", " + currentLoc.y + " and would have ended up at " +newPointCurvingWouldGive.x + ", " + newPointCurvingWouldGive.y);
        // }
        if (sHelper.shouldCurve(relAngleWouldNeedToGoIn) && !sHelper.closeToBoundary(currentLoc, dt)/*&& isInsideBox(newPointCurvingWouldGive)*/) {
            return sHelper.getBestAbsoluteDirection(currentLoc, goalLoc);
        } else {
            return Point.getDirection(currentLoc, goalLoc);
        }
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
