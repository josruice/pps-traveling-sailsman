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
    HashMap<Point, List<Point>> nnMap;
    FrequencyBucket currentBucket;
    int currentBucketi;
    int currentBucketj;
    int currentTargetNum;
    Point currentDestination;

    FrequencyBucket[][] frequencyBucket;


    public void calculateNearestNeighbors() {
        final int K = 5;
        for (int i = 0; i < this.targets.size(); i++) {
            Point target = targets.get(i);
            PriorityQueue<Point> neighbors = new PriorityQueue<Point>(5, new Comparator<Point>() {
                @Override
                public int compare(Point o1, Point o2) {
                    return new Double(approximateTimeToTarget(target, o2)).compareTo(approximateTimeToTarget(target, o1));
                }
            });
            for (int j = 0; j < this.targets.size(); j++) {
                if (i == j) {
                    continue;
                }
                Point neighbor = targets.get(j);
                neighbors.add(neighbor);
                if (neighbors.size() > K) {
                    neighbors.poll();
                }
            }
            List<Point> neighborsList = new ArrayList<Point>(neighbors);
            this.nnMap.put(target, neighborsList);
        }
    }

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

    private int estimatedValue(Point target, int targetNum) {
        if (targetNum == -1) {
            targetNum = this.targets.indexOf(target);
        }
        if (visited_set.get(id).contains(targetNum)) {
            return 0;
        }
        int value = numPlayers;
        for (int p = 0; p < numPlayers; p++) {
            if (playerWillReachTargetFirst(p, target, targetNum)) {
                value--;
            }
        }
        return value;
    }

    private double nnAdjustment(Point target, int targetNum) {
        List<Point> neighbors = nnMap.get(target);
        double total = 0.0;
        for (Point neighbor : neighbors) {
            total += estimatedValue(neighbor, -1) / Math.sqrt(approximateTimeToTarget(target, neighbor));
        }
        return total / neighbors.size();
    }

    private List<Double> getTargetWeights() {
        List<Double> weights = new ArrayList<Double>();
        for (int i = 0; i < targets.size(); i++) {
            if (visited_set.get(id).contains(i)) {
                weights.add(-1.0);
            } else {
                Point target = targets.get(i);
                int value = estimatedValue(target, i);
                value += nnAdjustment(target, i);
                weights.add(value / Math.sqrt(approximateTimeToTarget(groupLocations.get(id), target)));
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
    
    private Point getNextTarget() {
        Point max = null;
        double maxweight = 0.0;
        for (Map.Entry<Integer, Set<Integer>> entry : currentBucket.contained_points.entrySet()) {
            double value = estimatedValue(targets.get(entry.getKey()), entry.getKey());
            double weight = value / approximateTimeToTarget(groupLocations.get(id), targets.get(entry.getKey()));
            if (weight > maxweight) {
                maxweight = weight;
                max = targets.get(entry.getKey());
                currentTargetNum = entry.getKey();
            }
        }
        return max;
    }
    
    private void getNextBucket() {
        int initiali = (currentBucketi == 0) ? currentBucketi : currentBucketi - 1;
        int initialj = (currentBucketj == 0) ? currentBucketj : currentBucketj - 1;
        FrequencyBucket max = frequencyBucket[initiali][initialj];
        int maxi = initiali;
        int maxj = initialj;
        for (int i = initiali; i < initiali + 3 && i < 4; i++) {
            for (int j = initialj; j < initialj + 3 && j < 4; j++) {
                FrequencyBucket bucket = frequencyBucket[i][j];
                if (bucket.total_score > max.total_score) {
                    max = bucket;
                    maxi = i;
                    maxj = j;
                }
            }
        }
        if (max.total_score == 0) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    FrequencyBucket bucket = frequencyBucket[i][j];
                    if (bucket.total_score > max.total_score) {
                        max = bucket;
                        maxi = i;
                        maxj = j;
                    }
                }
            }
        }
        if (max.total_score == 0) {
            this.currentBucket = null;
        }
        this.currentBucket = max;
        int currentBucketi = maxi;
        int currentBucketj = maxj;
    }
    
    private Point getNextDestination() {
        if (currentBucket == null) {
            return initial;
        }
        if (currentBucket.total_score > 0) {
            return getNextTarget();
        } else {
            getNextBucket();
            if (currentBucket == null) {
                return initial;
            }
            return getNextTarget();
        }
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

      String temp = "speed_off_center";
      switch (temp) {
          case "geo_center" :
              initial = new Point((double) 5,(double) 5);
              break;
          case "geo_off_center" :
              initial = new Point(5.0 + gen.nextDouble(), 5.0 + gen.nextDouble());
              break;
          case "corner" :
              if(wind_direction.x < 0){
                  if(wind_direction.y < 0){
                      initial = new Point(7.5,7.5);
                  } else {
                      initial = new Point(7.5,2.5);
                  }
              } else {
                if(wind_direction.y < 0){
                    initial = new Point(2.5,7.5);
                } else {
                    initial = new Point(2.5,2.5);
                }
              }
              break;
          case "speed_off_center" :
              initial = new Point(5.0 + 2*wind_direction.x, 5.0 + 2*wind_direction.y);
              break;
          default :
              initial = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
              break;
        }

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
        this.nnMap = new HashMap<Point, List<Point>>();
        calculateNearestNeighbors();

        this.frequencyBucket = new FrequencyBucket[4][4];
        for (int i = 0; i < this.frequencyBucket[0].length; i++){
          for (int j = 0; j < this.frequencyBucket.length; j++){
            double lower_x = ((double) i ) * 2.5;
            double upper_x = ((double) i + 1 ) * 2.5;
            double lower_y = ((double) j ) * 2.5;
            double upper_y = ((double) j + 1 ) * 2.5;
            this.frequencyBucket[i][j] = new FrequencyBucket(this.numPlayers, this.id, lower_x, upper_x, lower_y, upper_y);
          }
        }

        initiateFrequencyBucket(targets);
        updateFrequencyBucket(visited_set);
        getNextBucket();
        currentTargetNum = -1;
    }

    public void initiateFrequencyBucket(List<Point> targets){
      for (int i = 0; i < this.frequencyBucket[0].length; i++){
        for (int j = 0; j < this.frequencyBucket.length; j++){
          this.frequencyBucket[i][j].initBucket(targets);
        }
      }
    }

    public void updateFrequencyBucket(Map<Integer, Set<Integer>> visited_set){
      for (int i = 0; i < this.frequencyBucket[0].length; i++){
        for (int j = 0; j < this.frequencyBucket.length; j++){
          this.frequencyBucket[i][j].updateBucket(visited_set);
        }
      }
    }

    public void printFrequencyBucket(){
      for (int i = 0; i < this.frequencyBucket[0].length; i++){
        for (int j = 0; j < this.frequencyBucket.length; j++){
          System.out.print(this.frequencyBucket[i][j]);
        }
      }
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
        } else{
          return moveToDestination(dt);
        }
    }

	private Point moveToDestination(double dt){
		Point pos;
		if(groupLocations == null){
			pos = initial;
		}else{
			pos = groupLocations.get(id);
    }
        if (currentTargetNum == -1 || visited_set.get(id).contains(currentTargetNum)) {
            currentDestination = getNextDestination();
        }
        Point target = currentDestination;
		double straightAngle = Point.angleBetweenVectors(pos, wind_direction);
		double bestDist = approximateTimeToTarget(pos, target);
		Point bestPoint = target;
		double perpAngle = Point.getNorm(wind_direction);

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
        System.out.println(visited_set);
        this.visited_set = visited_set;
        updateFrequencyBucket(visited_set);
    }
}