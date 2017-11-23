package sail.g3;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    int numPlayers;
    Point initial;
    Point wind_direction;
    List<Point> prevGroupLocations;
    List<Point> groupMoves;
    List<Point> groupLocations;
	int currentTargetIdx;
    
    public boolean playerWillReachTargetFirst(int p, Point target, int targetNum) {
        if (visited_set.get(p).contains(targetNum)) {
            return true;
        }
        if (!playerMovingToTarget(p, target)) {
            return false;
        }
        if (approximateTimeToTarget(groupLocations.get(p), target) < approximateTimeToTarget(groupLocations.get(id), target)) {
            return true;
        }
        return false;
    }
    
    private double approximateTimeToTarget(Point pos, Point target) {
        Point towardTarget = Point.getDirection(pos, target);
        double speed = Simulator.getSpeed(towardTarget, wind_direction);
        
        return Point.getDistance(pos, target) / speed;
    }
    
    private boolean playerMovingToTarget(int player, Point target) {
        Point playerMove = groupMoves.get(player);
        Point prevPlayerLoc = prevGroupLocations.get(player);
        Point towardTarget = Point.getDirection(prevPlayerLoc, target);
        
        return Math.abs(Point.angleBetweenVectors(playerMove, towardTarget)) < Math.PI / 6.0;
    }
    
    private List<Double> getTargetWeights() {
        List<Double> weights = new ArrayList<Double>();
        for (int i = 0; i < targets.size(); i++) {
            if (visited_set.get(id).contains(i)) {
                weights.add(-1.0);
            } else {
                Point target = targets.get(i);
                int value = numPlayers;
                for (int p = 0; p < numPlayers; p++) {
                    if (playerWillReachTargetFirst(p, target, i)) {
                        value--;
                    }
                }
                weights.add(value / approximateTimeToTarget(groupLocations.get(id), target));
            }
        }
        return weights;
    }
    
    private List<Point> calculateMoves(List<Point> initialLocations, List<Point> finalLocations) {
        ArrayList<Point> moves = new ArrayList<Point>();
        for (int i = 0; i < initialLocations.size(); i++) {
            Point init = initialLocations.get(i);
            Point fin = finalLocations.get(i);
            Point move = new Point(fin.x - init.x, fin.y - init.y);
            moves.add(move);
        }
        return moves;
    }
	
	private int getClosestTarget(){
		int smallestIdx = 0;
	
	/*	for(int i = 1; i<targets.size(); i++){
			double time = approximateTimeToTarget(initial, targets.get(i));
			if(time<approximateTimeToTarget(groupLocations.get(id), targets.get(i))){
				//smallest = time;
				smallestIdx = i;
			}
			
		}*/
		return 0;
	}

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        this.wind_direction = wind_direction;
        gen = new Random(seed);
        initial = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.numPlayers = group_locations.size();
        this.prevGroupLocations = group_locations;
        this.visited_set = new HashMap<>();
        for (int i = 0; i < numPlayers; i++) {
            visited_set.put(i, new HashSet<Integer>());
        }
		this.currentTargetIdx = getClosestTarget();
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        this.groupMoves = calculateMoves(this.prevGroupLocations, group_locations);
        this.groupLocations = group_locations;
		
		
        // testing timeouts... 
        // try {
        //     TimeUnit.MILLISECONDS.sleep(1);
        // } catch(Exception ex) {
        //     ;
        // }
        // just for first turn
        prevGroupLocations = group_locations;
        if(visited_set.get(id).size() == targets.size()) {
            //this is if finished visiting all
            return Point.getDirection(group_locations.get(id), initial);
        } 
		else{ 
            //if we reached last target, pick a new target
			if(visited_set.get(id).contains(currentTargetIdx))
			{
				List<Double> weights = getTargetWeights();
				
				int highestIdx = 0;
				for(int i = 0; i < weights.size(); i++){
					if(weights.get(i) > weights.get(highestIdx))
						highestIdx = i;
				}
				currentTargetIdx = highestIdx;
			}
            return moveToTarget(dt);
        }
    }
	
	private Point moveToTarget(double dt){
		Point pos;
		if(groupLocations == null)
			pos = initial;
		else	
			pos = groupLocations.get(id);
		Point target = targets.get(currentTargetIdx);
		double straightAngle = Point.angleBetweenVectors(pos, wind_direction);
		double bestDist = approximateTimeToTarget(pos, target);
		Point bestPoint = target;
		
		Point perp = Point.rotateCounterClockwise(wind_direction,1.59);	
		double perpAngle = Point.angleBetweenVectors(target, perp);
		
		for(double i = straightAngle; i<=perpAngle; i+= .1){
			double x = 2.5 * Math.cos(i) - 0.5;
			double y = 5 * Math.sin(i);
			double newX = pos.x + (x*dt);
			double newY = pos.y + (y*dt);
			
			Point p = new Point(newX, newY);
			
			double testDist = approximateTimeToTarget(pos, p);
			testDist += approximateTimeToTarget(p, target);
			
			if(testDist <= bestDist){
				bestDist = testDist;
				bestPoint = p;
			}
		}
		
		return Point.getDirection(groupLocations.get(id), bestPoint);
		
	
	}

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}