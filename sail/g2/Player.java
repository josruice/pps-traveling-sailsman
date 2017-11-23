package sail.g2;

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
    List<Integer> target_id_seq;
    int num_visited;
    Point current_target;
    int num_players;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        // if(t >= 10){
        // //random initialization
        //     initial = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        // }
        //else{
        //center square initialization
        initial = new Point(4.5 + gen.nextDouble()*1, 4.5 + gen.nextDouble()*1);
        //}
        double speed = Simulator.getSpeed(initial, wind_direction);
        this.wind_direction = wind_direction;
        return initial;

    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        //this.targets = new ArrayList<Point>();
        this.targets = targets;

        int result = -1;
        double d = -1;
        for(int i = 0; i < this.targets.size(); i++){
            double dist = getDistance(this.initial, this.targets.get(i));
            if(dist < d || d == -1){
                d = dist;
                result = i;
            }
        }
        this.current_target = this.targets.get(result);
       
        this.id = id;
        this.num_visited = 0;
        this.num_players = group_locations.size();
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        // testing timeouts... 
        // try {
        //     TimeUnit.MILLISECONDS.sleep(1);
        // } catch(Exception ex) {
        //     ;
        // }
        // just for the last turn 
        if(visited_set != null && visited_set.get(id).size() == targets.size()) {
            //this is if finished visiting all
            this.current_target =  this.initial;
        } else{
            //pick a target
            //if(visited_set != null && visited_set.get(id).size() > this.num_visited){
            int max_visit = 0;
            for(int i = 0; i < this.num_players; i++){
                if(visited_set != null && visited_set.get(i).size() > max_visit){
                    max_visit = visited_set.get(i).size();
                }
            }
            if(visited_set != null && max_visit < this.targets.size()*0.8){//greedy distance with weights
                this.current_target = computeTarget(group_locations.get(id),visited_set);
                //System.out.println("weight");
            }
            else if(visited_set != null){//simple greedy distance
                 this.current_target = computeTargetEnd(group_locations.get(id),visited_set);
                 //System.out.println("dist");
            }

        }
            double max_proj = 0;
            Point target = Point.getDirection(
                group_locations.get(id),
                this.current_target);
            Point result = target;
            //optimize the angle to the target
            for(int i = -90; i <= 90; i+=1){
                double tmp_angle = i * Math.PI / 180;
                Point tmp_direction = angleToDirection(target, tmp_angle);
                tmp_direction = tmp_direction.getUnitVector(tmp_direction);
                double speed = getSpeed(tmp_direction,wind_direction);
                Point distanceMoved = new Point(
                    tmp_direction.x * speed * dt, 
                    tmp_direction.y * speed * dt
                );
                Point nextLocation = Point.sum(group_locations.get(id), distanceMoved);
                if(nextLocation.x >= 0 && nextLocation.y <= 10 &&
            nextLocation.y >= 0 && nextLocation.x <= 10) {
                    double proj = getProjection(tmp_direction, target, wind_direction);
                    //System.out.println(proj);
                    if(proj > max_proj){
                        //System.out.println("find better direction! " + tmp_direction.x + " " + tmp_direction.y);
                        max_proj = proj;
                        result = tmp_direction;
                    }
                }
            }
            //System.out.println("target: " + target.x + " " + target.y + " our choice: " + result.x + " " + result.y);
            return result;
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }


    public double getSpeed(Point p, Point wind_direction) {
        if(Point.getNorm(p)==0) return 0;
        double angle = Point.angleBetweenVectors(p, wind_direction) + Math.PI;
        double x = 2.5 * Math.cos(angle) - 0.5;
        double y = 5 * Math.sin(angle);
        return Math.sqrt((x)*(x) + (y)*(y));
    }

    public double getProjection(Point p, Point target_direction, Point wind_direction){
        double speed = getSpeed(p,wind_direction);
        double angle = Point.angleBetweenVectors(p, target_direction);
        return speed*Math.cos(angle);
    }

    //counterclock wise is positive
    public Point angleToDirection(Point start, double angle){
        return new Point(start.x*Math.cos(angle) - start.y*Math.sin(angle), start.x*Math.sin(angle) + start.y*Math.cos(angle));
    } 

    public double getDistance(Point first, Point second) {
        double dist_square = (first.x - second.x)*(first.x - second.x) + (first.y - second.y)*(first.y - second.y);
        double dist = Math.sqrt(dist_square);
        return dist;
    }

    public Point computeTarget(Point loc, Map<Integer, Set<Integer>> visited_set){
        int result = -1;
        double d = -1;
        for(int i = 0; i < this.targets.size(); i++){
            if(!visited_set.get(id).contains(i)){
                int count = 0;
                for(int k = 0 ; k < this.num_players; k++){
                    if(visited_set.get(k).contains(i)){
                        count+=1;
                    }
                }
                //take into consideration about the number of visits to the target
                double dist = getDistance(loc, this.targets.get(i)) + (count - this.num_players);
                if(dist < d || d == -1){
                    d = dist;
                    result = i;
                }
            }
        }
        return new Point(this.targets.get(result).x, this.targets.get(result).y);
    }
    public Point computeTargetEnd(Point loc, Map<Integer, Set<Integer>> visited_set){
        int result = -1;
        double d = -1;
        for(int i = 0; i < this.targets.size(); i++){
            if(!visited_set.get(id).contains(i)){
                //take into consideration about the number of visits to the target
                double dist = getDistance(loc, this.targets.get(i));
                if(dist < d || d == -1){
                    d = dist;
                    result = i;
                }
            }
        }
        return new Point(this.targets.get(result).x, this.targets.get(result).y);
    }
}
