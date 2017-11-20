package sail.g5;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    Point wind_direction;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        initial = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        double speed = Simulator.getSpeed(initial, wind_direction);
        this.wind_direction = wind_direction;
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {      
        return greedyMove(group_locations, id, dt, time_remaining_ms);
    }

    // Applies the Greedy Strategy to Find the next Point
    public Point greedyMove(List<Point> group_locations, int id, double dt, long time_remaining_ms){
        Point myCurrentLocation = group_locations.get(id);
        int nTargets = targets.size();
        Set<Integer> unVisitedTargets = new HashSet<Integer>();

        
        for(int i = 0; i < nTargets; i ++){
            if (!visited_set.get(id).contains(i))
                unVisitedTargets.add(i);
        }

        if (unVisitedTargets.size() == 0)
            return Point.getDirection(myCurrentLocation, initial);


        double minTime = Double.MAX_VALUE;
        int closestTarget = 0;
        for (int unvisitedTargetPoint : unVisitedTargets){
            double distance = Point.getDistance(myCurrentLocation, (Point) targets.get(unvisitedTargetPoint));
            Point direction = Point.getDirection(myCurrentLocation, (Point) targets.get(unvisitedTargetPoint));
            double speed = getSpeed(direction, wind_direction);
            double time = distance/speed;

            if (time < minTime){
                minTime = time;
                closestTarget = unvisitedTargetPoint;
            }
        }

        return Point.getDirection(myCurrentLocation, (Point) targets.get(closestTarget));
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }

    public static double getSpeed(Point p, Point wind_direction) {
        if(Point.getNorm(p)==0) 
            return 0;
        double angle = Point.angleBetweenVectors(p, wind_direction) + Math.PI;
        double x = 2.5 * Math.cos(angle) - 0.5;
        double y = 5 * Math.sin(angle);
        return Math.sqrt((x)*(x) + (y)*(y));
  }
}
