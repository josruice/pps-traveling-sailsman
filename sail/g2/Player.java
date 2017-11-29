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
    boolean second_stage = false;
    Map<Integer, List<Double>> target_dist_map;
    Map<Integer, Integer> targets_visited;

    int curr_target_ind = -1;


    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        // if(t >= 10){
        // //random initialization
        //      initial = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        // }
        // else{
        //center square initialization
         //initial = new Point(4.5 + gen.nextDouble(), 4.5 + gen.nextDouble());
    // }


        Point unit = Point.getUnitVector(wind_direction);
        initial = new Point(4, unit.x / unit.y+5);
        if(initial.y > 10 || initial.y < 0 || initial.x > 10 || initial.x < 0){
            //System.out.println("will be at the center");
            initial = new Point(4.5 + gen.nextDouble(), 4.5 + gen.nextDouble());
        }





        // if(initial.y > 10){
        //     System.out.println( "start point "+initial.x + " " + initial.y);
        //     initial = new Point(4+ gen.nextDouble()*1, unit.x / unit.y-5+ gen.nextDouble()*1 );
        // }
        // if (initial.y < 0){
        //     System.out.println( "start point "+initial.x + " " + initial.y);
        //     initial = new Point(4+ gen.nextDouble()*1, unit.x / unit.y+10+ gen.nextDouble()*1 );
        // }
        
        //System.out.println( "start point "+initial.x + " " + initial.y);
        this.wind_direction = wind_direction;
        return initial;

    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        //this.targets = new ArrayList<Point>();
        this.targets = targets;
        this.target_dist_map = new HashMap<Integer, List<Double>>();
        this.targets_visited = new HashMap<Integer, Integer>();
        int result = -1;
        double tempTime = -1;
        for(int i = 0; i < this.targets.size(); i++){
                //compute time to target
            this.targets_visited.put(i, 0);
            List<Double> tmp_time = new ArrayList<Double>();
            for(int j = 0; j < this.targets.size(); j++){
                double dist2 = Point.getDistance(this.targets.get(i), this.targets.get(j));
                double speed2 = Simulator.getSpeed(Point.getDirection(this.targets.get(i), this.targets.get(j)), wind_direction);
                double time2 = dist2/speed2;
                tmp_time.add(time2);
            }
            //System.out.println(tmp_time);
            this.target_dist_map.put(i, tmp_time);

            double dist = Point.getDistance(this.initial, this.targets.get(i));
            double speed = Simulator.getSpeed(Point.getDirection(this.initial, targets.get(i)), wind_direction);
            double time = dist/speed;

            //get shortest time
            if(tempTime > time || tempTime == -1) {
                tempTime = time;
                result = i;
            }
        }
        this.curr_target_ind = result;
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
            //this is if finished visiting all targets
            this.current_target =  this.initial;
        } else{
            //pick a target
            //if(visited_set != null && visited_set.get(id).size() > this.num_visited){
            // int max_visit = 0;
            // for(int i = 0; i < this.num_players; i++){
            //     if(visited_set != null && visited_set.get(i).size() > max_visit){
            //         max_visit = visited_set.get(i).size();
            //     }
            // }
            if(visited_set != null && this.num_visited < visited_set.get(id).size()){
                // boolean change = 0;
                // while(this.current_tar.size() != 0){
                //     if(this.current_target.get(0))
                // }
                this.current_target = computeTarget(group_locations,visited_set);
                this.num_visited = visited_set.get(id).size();
                //System.out.println("g2 has visited: " + visited_set.get(id).size() + " " + this.current_target.x + " " + this.current_target.y);
            }


        }
            double max_proj = 0;
            Point target = Point.getDirection(
                group_locations.get(id),
                this.current_target);
            //System.out.println(Point.angleBetweenVectors(target, wind_direction));
            Point result = target;
            //set temp time for comparing
            double tempTime = -1;
            //optimize the angle to the target
            //tuning degrees (we tried 90, 30)
            // for(int i = 0; i < this.targets.size(); i++){
            //     if(!visited_set.get(id).contains(i) && Point.getDistance(group_locations.get(id), this.targets.get(i)) <= dt*Point.getSpeed(target,wind_direction))){
            //         current_tar.add(0,this.targets.get(i));
            //         this.current_target = this.targets.get(i);
            //     }
            // }
            if(Point.angleBetweenVectors(target, wind_direction) < Math.PI / 6 || Point.angleBetweenVectors(target, wind_direction) > Math.PI*11/6  ||
             (Point.angleBetweenVectors(target, wind_direction) > Math.PI*5/6 && Point.angleBetweenVectors(target, wind_direction) < Math.PI*7/6 )){
            for(int i = -90; i <= 90; i+=2){
                double tmp_angle = i * Math.PI / 180;
                Point tmp_direction = angleToDirection(target, tmp_angle);
                tmp_direction = Point.getUnitVector(tmp_direction);
                double speed = getSpeed(tmp_direction,wind_direction);
                Point distanceMoved = new Point(
                    tmp_direction.x * speed * dt,
                    tmp_direction.y * speed * dt
                );
                Point nextLocation = Point.sum(group_locations.get(id), distanceMoved);
                if(nextLocation.x >= 0 && nextLocation.y <= 10 &&
            nextLocation.y >= 0 && nextLocation.x <= 10) {

                  //optimize base on time
                    double dist = Point.getDistance(nextLocation, this.current_target);
                    double speed2 = Simulator.getSpeed(Point.getDirection(nextLocation, this.current_target), wind_direction);
                    double time = dist/speed2;
                    if (tempTime > time || tempTime ==-1) {
                      tempTime = time;
                      result = tmp_direction;
                    }
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

    // this is to maximize the speed to decrease distance TO the target
    // i.e. how to move to the target in shortest time
    public double getProjection(Point p, Point target_direction, Point wind_direction){
        double speed = getSpeed(p,wind_direction);
        double angle = Point.angleBetweenVectors(p, target_direction);
        return speed*Math.cos(angle);
    }

    // counterclock wise is positive
    public Point angleToDirection(Point start, double angle){
        return new Point(start.x*Math.cos(angle) - start.y*Math.sin(angle), start.x*Math.sin(angle) + start.y*Math.cos(angle));
    }

    public Point computeTarget(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set){
        int result = -1;
        Point loc = group_locations.get(id);
        double d = -1;
        double tempTime = -1;
        for(int i = 0; i < this.targets.size(); i++){
                int count = 0;
                for(int k = 0 ; k < this.num_players; k++){
                    if(visited_set.get(k).contains(i)){
                        count+=1;
                    }
                }
                this.targets_visited.put(i, count);
        }
        for(int i = 0; i < this.targets.size(); i++){
             if(!visited_set.get(id).contains(i)){
                //compute time to target
                double dist = Point.getDistance(loc, this.targets.get(i));
                double speed = Simulator.getSpeed(Point.getDirection(loc, targets.get(i)), wind_direction);
                double time = (dist)/speed;

                // if(second_stage ==false){
                    //count how many other players are around the target
                    int count1 = 0;
                    for(int j = 0; j < num_players; j++){
                        //if(visited_set.get(j).size() >= 0.8*this.targets.size()) second_stage = true;
                        if(j != id && !visited_set.get(j).contains(i)){
                            double dist1 = Point.getDistance(group_locations.get(j), this.targets.get(i));
                            double speed1 = Simulator.getSpeed(Point.getDirection(group_locations.get(j), targets.get(i)), wind_direction);
                            double time1 = (dist1)/speed1 ;
                            // double time1 = this.target_dist_map.get(i).get(j);
                            if(time1 < time) count1+=1;
                        }

                    }
                    int count2 = 0;
                        List<Double> neighbor_time = this.target_dist_map.get(this.curr_target_ind);
                        for(int p = 0; p < neighbor_time.size(); p++){
                            if(!visited_set.get(id).contains(p) && p!=i){
                                if(neighbor_time.get(p) < 0.5*time){
                                    count2+=1;
                                }
                            }
                        }
                    //think about the next target
                    List<Double> next_time = this.target_dist_map.get(i);
                    int next_ind = -1;
                    double next_t = -1;
                    
                    for(int p = 0; p < next_time.size(); p++){
                        if(!visited_set.get(id).contains(p) && p!=i){
                            if(((num_players-this.targets_visited.get(p))/next_time.get(p) < next_t && p != i) || next_t == -1){
                                next_t = (num_players-this.targets_visited.get(p))/next_time.get(p);
                                next_ind = p;
                            }
                        }
                    }
                    //maximizing measure
                    double measure = ((num_players-this.targets_visited.get(i)) - 0.7*count1 + 0.3*count2)/time;
                    if( measure + 0.3*next_t > tempTime || tempTime == -1){
                        tempTime = measure;
                        result = i;
                    }

            }
        }
        this.curr_target_ind = result;
        return new Point(this.targets.get(result).x, this.targets.get(result).y);
    }
   
}
