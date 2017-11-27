package sail.g1;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point wind_direction;
    Point initial;

    int nextTargetIndex;
    Point nextTargetLocation;


    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        initial = new Point(5, 5);
        this.wind_direction = wind_direction;
        double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.nextTargetIndex = -1;
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        return nearestNeighborMove(group_locations, id, dt, time_remaining_ms);
    }


    public Point nearestNeighborMove(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        if(nextTargetIndex == -1 || visited_set.get(id).contains(nextTargetIndex)) {
            return Point.getDirection(group_locations.get(id), findNearestNeighbor(group_locations.get(id)));
        } else {
            return Point.getDirection(group_locations.get(id), nextTargetLocation);
        }
    }


    /*
    private Point findNearestNeighborDistance(Point curLoc) {
        Point nearestTarget = null;
        double minDistance = Double.MAX_VALUE;
        for(int i = 0; i < targets.size(); i++) {
            if(visited_set != null && visited_set.get(this.id).contains(i)) continue;

            double dist = Point.getDistance(curLoc, targets.get(i));

            if(dist < minDistance) {
                nearestTarget = targets.get(i);
                minDistance = dist;
                nextTargetIndex = i;

            }
        }

        // No more neighbors so return to starting point
        if(nearestTarget == null) {
            nearestTarget = initial;
            nextTargetIndex = targets.size();
        }

        return nearestTarget;
    }
    */


    private Point findNearestNeighbor(Point curLoc) {
        Point nearestTarget = null;
        int targetIndex = -1;
        double minTime = Double.MAX_VALUE;
        for(int i = 0; i < targets.size(); i++) {
            if(visited_set != null && visited_set.get(this.id).contains(i)) continue;

            // Compute time to get to target
            double time = getTimeToTravel(curLoc, targets.get(i));
            if(nearestTarget != null && i != nextTargetIndex && Point.getDistance(targets.get(nextTargetIndex), targets.get(i)) <= 0.02) {
                nearestTarget = new Point((targets.get(i).x + targets.get(nextTargetIndex).x)/2, (targets.get(i).y +  targets.get(nextTargetIndex).y)/2);
                nextTargetIndex = i;
                minTime = getTimeToTravel(curLoc, nearestTarget);
            } else if(time < minTime) {
                nearestTarget = targets.get(i);
                minTime = time;
                nextTargetIndex = i;
            }
        }

        // No more neighbors so return to starting point
        if(nearestTarget == null) {
            nearestTarget = initial;
            nextTargetIndex = targets.size();
        }

        // Cache target point value
        nextTargetLocation = nearestTarget;

        //System.out.println(id + ":" + nearestTarget.x + "," +  nearestTarget.y);
        return nearestTarget;
    }

    private double getTimeToTravel(Point from, Point to) {
        return Point.getDistance(from, to)/Simulator.getSpeed(Point.getDirection(from, to), wind_direction);
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
