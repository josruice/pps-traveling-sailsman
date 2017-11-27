package sail.g5;
import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player_m extends sail.sim.Player {

    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    
    int next_target_index;
    Point next_target;
    Point wind_direction;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        initial = new Point(5, 5);
        double speed = Simulator.getSpeed(initial, wind_direction);
        this.wind_direction = wind_direction;
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.next_target_index = -1;
    }

    // find the travel time from two points
    private double travel_time(Point from, Point to) {
        return Point.getDistance(from, 
                to)/Simulator.getSpeed(Point.getDirection(from, to), wind_direction);
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
        if(next_target_index == -1 || visited_set.get(id).contains(next_target_index)) {
            return Point.getDirection(group_locations.get(id), 
                    get_next_neighbor(group_locations.get(id)));
        } else {
            return Point.getDirection(group_locations.get(id), next_target);
        }
    }

    private Point get_next_neighbor(Point present_location) {
        Point next_target_point = null;
        int goal = -1;
        double minimum_time = Double.MAX_VALUE;
        for(int i = 0; i < targets.size(); i++) {
            if(visited_set != null && 
                    visited_set.get(this.id).contains(i)) continue;
            double time = travel_time(present_location, targets.get(i));
            if(next_target_point != null && i != next_target_index && 
                    Point.getDistance(targets.get(next_target_index), targets.get(i)) <= 0.02) {
                next_target_point = new Point((targets.get(i).x + 
                            targets.get(next_target_index).x)/2, (targets.get(i).y +  
                            targets.get(next_target_index).y)/2);
                next_target_index = i;
                minimum_time = travel_time(present_location, next_target_point);
            } else if(time < minimum_time) {
                next_target_point = targets.get(i);
                minimum_time = time;
                next_target_index = i;
            }
        }

        // return to start
        if(next_target_point == null) {
            next_target_point = initial;
            next_target_index = targets.size();
        }
        
        next_target = next_target_point;
        return next_target_point;
    }

    /**
     *  visited_set.get(i) is a set of targets that the ith player has 
     visited.
     */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, 
            Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
