package sail.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class Player{
    public Player() {
    }

    /**
    *   Called at the start
    * @param wind_direction is a unit vector in the direction of wind
    */
    public abstract Point chooseStartingLocation(Point wind_direction, Long seed, int t);

    /**
    * Called after everyone has chosen their initial positions
    */
    public abstract void init(List<Point> group_locations, List<Point> targets, int id);
    
    /**
    * Called at the start of every turn, returns a unit vector in the direction you want to move to
    * 
    */
    public abstract Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms);

    /**
    * Called after everyone has moved
    * @param visited_set.get(i) stores the targets that the ith player has visited.
    */
    public abstract void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set);

}