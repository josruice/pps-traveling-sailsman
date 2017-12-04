package sail.g5;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visitedTargets; // For every player, store which targets he has visited.
    Map<Integer, Set<Integer>> unvisitedTargets; // For every player, store which target he has NOT visited.
    Map<Integer, Set<Integer>> playerVisitsByTarget; // For every target, store which players have visited it (been received).
    Set<Integer> ourUnvisitedTargets;
    Random gen;
    int id;
    int numTargets;
    int numPlayers;
    Long initialTimeRemaining;
    Point initialLocation;
    Point currentLocation;
    Point windDirection;
    
    
    MST mst;
    double[][] graph;
    Tree tree;
    ArrayList<Integer> path;
    

    final String INITIAL_POINT = "middle"; // One of: random, middle, windMiddle
    String STRATEGY = "weightedGreedy"; // One of: greedy, weightedGreedy, mst

    // Enable or disable different Weighted Greedy params, to compare results.
    final boolean WG_SCORE_ENABLED = true;
    final boolean WG_TIME_ENABLED = true;
    final boolean WG_PLAYERS_DISTANCES_ENABLED = true;

    // This one times out for t=100 and tl=1000, in the first few turns, so it is disabled automatically.
    final boolean WG_TARGETS_DISTANCES_ENABLED = true;

    @Override
    public Point chooseStartingLocation(Point windDirection, Long seed, int t) {
        this.numTargets = t;

        // Use seed, to obtain deterministic results wrt input randomness.
        gen = new Random(seed);

        this.windDirection = windDirection;
        
        if(this.numTargets >= 500){
        	STRATEGY = "mst";
        }
        
        switch (INITIAL_POINT) {
            case "random":
                initialLocation = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
                break;
            case "middle":
                initialLocation = new Point(5.0, 5.0);
                break;
            case "windMiddle":
                // The middle point wrt the wind lies in the line that follows the wind vector, upwind direction,
                // at 1/3 of the distance from the center (5,5) to the edges of the board.
                // To compute it, first we need the unit vector that represents the wind direction.
                Point unitWind = Point.getUnitVector(windDirection);

                // Now, we need the distance of the edges of the board in this direction. If the angle of the wind
                // wrt to the x-axis is multiple of 90º, then the solution is trivial, since the distance is 5 km.
                // For the rest of the cases, we need some trigonometry.
                Point xAxisVector = new Point(1,0);

                // Rotate the wind so that it is a vector in the first quadrant. This works because of symmetry and
                // it helps to simplify things.
                Point absUnitWind = new Point(Math.abs(unitWind.x), Math.abs(unitWind.y));

                double alpha = Point.angleBetweenVectors(xAxisVector, absUnitWind);
                double distanceToEdge = 5.0;
                if (alpha <= Math.PI / 4) { // alpha <= 45º
                    distanceToEdge /= Math.cos(alpha);
                } else { // 45º < alpha <= 90º
                    distanceToEdge /= Math.sin(alpha);
                }

                // Now we have everything we need.
                initialLocation = new Point(
                        5 + (1./3 * distanceToEdge) * unitWind.x,
                        5 + (1./3 * distanceToEdge) * unitWind.y
                );
                break;
            default:
                System.err.println("Invalid initial point "+INITIAL_POINT+" chosen");
                initialLocation = new Point(0, 0);
        }
        return initialLocation;
    }

    @Override
    public void init(List<Point> groupLocations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.numPlayers = groupLocations.size();
        this.initialTimeRemaining = null;

        // Initialize unvisited targets by player map.
        this.unvisitedTargets = new HashMap<Integer, Set<Integer>>();
        for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
            Set<Integer> playerUnvisited = new HashSet<Integer>();
            for (int i = 0; i < this.numTargets; ++i) {
                playerUnvisited.add(i);
            }
            this.unvisitedTargets.put(playerId, playerUnvisited);
        }
        this.ourUnvisitedTargets = this.unvisitedTargets.get(this.id);

        // Initialize player visits by target map.
        this.playerVisitsByTarget = new HashMap<Integer, Set<Integer>>();
        for (int targetId = 0; targetId < this.numTargets; ++targetId) {
            Set<Integer> playerVisits = new HashSet<Integer>();
            this.playerVisitsByTarget.put(targetId, playerVisits);
        }
        
        
        if(STRATEGY == "mst"){
        	ArrayList<Point> targetsClone = new ArrayList<Point>();
        	for(Point target: targets){
        		targetsClone.add(target);
        	}
        	targetsClone.add(initialLocation);
        	
	        mst = new MST();
	        graph = new double[numTargets + 1][numTargets + 1];
	        for(int i = 0; i < numTargets + 1; i++){
	        	for(int j = 0; j < numTargets + 1; j++){
	        		graph[i][j] = computeEstimatedTimeToTarget(targetsClone.get(i), targetsClone.get(j));
	        	}
	        }
	        int[] parents = mst.primMST(graph);
	        buildTree(parents);
	        path = new ArrayList<Integer>();
	        tree.preorder(path);
	        
	        path.remove(0);
        }
    }

    @Override
    public Point move(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs) {
        if (this.initialTimeRemaining == null) this.initialTimeRemaining = timeRemainingMs;
        this.currentLocation = groupLocations.get(id);
        switch (STRATEGY) {
            case "greedy":
                return greedyMove(groupLocations, id, timeStep, timeRemainingMs);
            case "weightedGreedy":
                return weightedGreedyMove(groupLocations, id, timeStep, timeRemainingMs);
            case "mst":
            	return mstMove(groupLocations, id, timeStep, timeRemainingMs);
            default:
                System.err.println("Invalid strategy "+STRATEGY+" chosen");
                return new Point(0,0);
        }
    }

    public Point weightedGreedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        // Let's get the maximum weight unvisited target according to the following formula (if all params are enabled):
        //  targetWeight =
        //      targetRemainingScore * distanceFromNonVisitedPlayers
        //     ——————————————————————————————————————————————————————
        //         timeToTarget * weightedDistanceToOtherTargets

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : this.ourUnvisitedTargets) {
            double weight = 1.0;

            if (WG_SCORE_ENABLED) {
                int score = computeRemainingScore(targetId);
                weight *= score;
            }

            if (WG_TIME_ENABLED) {
                double ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
                weight /= ourTime;
            }

            if (WG_PLAYERS_DISTANCES_ENABLED) {
                double othersTime = computeUnvisitedPlayersTimeTo(groupLocations, targetId);
                double avgPlayersTime = othersTime / this.numPlayers;
                weight *= avgPlayersTime;
            }

            boolean wontTimeout =
                    (this.numTargets <= 20) || (this.numTargets <= 100 && this.initialTimeRemaining > 9800);
            if (WG_TARGETS_DISTANCES_ENABLED && wontTimeout) {
                double weightedTargetsTime = computeWeightedTimeToOtherTargets(targetId, this.targets);
                weight /= weightedTargetsTime;
            }

            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }

        return computeNextDirection(maxWeightTarget, timeStep);
    }

    // Applies the Greedy Strategy to choose the next target, only considering the time to reach it.
    public Point greedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        Point nextTarget = initialLocation; // If no unvisited targets, initial location will be our next target.

        double minTime = Double.MAX_VALUE;
        for (int unvisitedTargetPoint : this.ourUnvisitedTargets) {
            double time = computeEstimatedTimeToTarget(targets.get(unvisitedTargetPoint));
            if (time < minTime) {
                minTime = time;
                nextTarget = targets.get(unvisitedTargetPoint);
            }
        }

        return computeNextDirection(nextTarget, timeStep);
    }
    
    public Point mstMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
    	while(path.size() > 0 && !ourUnvisitedTargets.contains(path.get(0)))
    		path.remove(0);
    	
    	if(path.size() == 0){
    		Point direction = Point.getDirection(currentLocation,initialLocation);
    		Point unitDirection = Point.getUnitVector(direction);
        	return unitDirection;    		
    	}
    	
    	int targetIndex = path.get(0);
    	Point target = targets.get(targetIndex);
    	Point nextTarget = initialLocation;
    	if(path.size() >= 2){
        	int nextTargetIndex = path.get(1);
        	nextTarget = targets.get(nextTargetIndex);
    	}
    	
    	Point directionBetweenTargets = Point.getDirection(target,nextTarget);
    	Point unitDirectionBetweenTargets = Point.getUnitVector(directionBetweenTargets);
    	Point pointWithin10MetersDirection = Point.sum(target,Point.multiply(unitDirectionBetweenTargets,0.01));
    	
    	return computeNextDirection(pointWithin10MetersDirection,timeStep);
    }
    
    public void buildTree(int[] parents){    	
    	tree = new Tree();
    	int rootIndex = findRootIndexOfMST(parents);
    	tree.root = new Node(rootIndex);
    	tree.root.children = findChildren(rootIndex, parents);    	
    }
    
    public int findRootIndexOfMST(int[] parents){
    	for(int i = 0; i < numTargets; i++){
    		if(parents[i] == -1){
    			return i;
    		}
    	}
    	return -1;
    }
    
    public ArrayList<Node> findChildren(int rootIndex, int[] parents){
    	ArrayList<Node> children = null;
    	
    	for(int i = 0; i < parents.length; i++){
    		if(parents[i] == rootIndex){
    			Node child = new Node(i);
    			child.children = findChildren(i, parents);
    			
    			if(children == null)
    				children = new ArrayList<Node>();
    			children.add(child);
    		}
    	}
    	
    	return children;
    }

    private double computeWeightedTimeToOtherTargets(int targetId, List<Point> targets) {
        // It is weighted because it also takes into account the remaining score of those targets.
        double weightedTime = 1.0;
        Point thisTarget = targets.get(targetId);
        for (int i : this.ourUnvisitedTargets) {
            if (i == targetId) continue;  // Skip itself.
            Point otherTarget = targets.get(i);
            int remainingScore = computeRemainingScore(i);
            weightedTime += computeEstimatedTimeToTarget(thisTarget, otherTarget) / remainingScore;
        }
        return weightedTime;
    }

    private double computeUnvisitedPlayersTimeTo(List<Point> groupLocations, int targetId) {
        double distance = 1.0;
        Point target = this.targets.get(targetId);
        for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
            if (playerId == this.id) continue; // Skip our own.
            if (!this.playerVisitsByTarget.get(targetId).contains(playerId)) {
                // This means that this player hasn't visited this target yet, so
                // compute her time to target.
                Point player = groupLocations.get(playerId);
                distance += computeEstimatedTimeToTarget(player, target);
            }
        }
        return distance;
    }

    private int computeRemainingScore(int targetId) {
        return this.numPlayers - this.playerVisitsByTarget.get(targetId).size();
    }

    private double computeEstimatedTimeToTarget(Point target) {
        return computeEstimatedTimeToTarget(this.currentLocation, target);
    }

    private double computeEstimatedTimeToTarget(Point point, Point target) {
        double distance = Point.getDistance(point, target);
        Point direction = Point.getDirection(point, target);
        double speed = Simulator.getSpeed(direction, windDirection);
        return distance/speed;
    }

    private Point computeNextDirection(Point target, double timeStep) {
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        Point perpendicularLeftDirection = Point.rotateCounterClockwise(directionToTarget, Math.PI/2.0);
        Point perpendicularRightDirection = Point.rotateCounterClockwise(directionToTarget, -Math.PI/2.0);
        return findBestDirection(perpendicularLeftDirection, perpendicularRightDirection, target, 100, timeStep);
    }

    private Point findBestDirection(Point leftDirection, Point rightDirection, Point target, int numSteps,
                                    double timeStep) {
        // First, check if the target is reachable in one time step from our current position.
        double currentDistanceToTarget = Point.getDistance(this.currentLocation, target);
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        double speedToTarget = Simulator.getSpeed(directionToTarget, this.windDirection);
        double distanceWeCanTraverse = speedToTarget * timeStep;
        double distanceTo10MeterAroundTarget = currentDistanceToTarget-0.01;
        if (distanceWeCanTraverse > distanceTo10MeterAroundTarget) {
            return directionToTarget;
        }

        // If that is not the case, choose the direction that will get us to a point where the time to reach
        // the target is minimal, if going directly to the target.
        double minTimeToTarget = Double.POSITIVE_INFINITY;
        Point minTimeToTargetDirection = null;

        double totalRadians = Point.angleBetweenVectors(leftDirection, rightDirection);
        double radiansStep = totalRadians / (double) numSteps;
        for (double i = 0.0; i < totalRadians; i+=radiansStep) {
            Point direction = Point.rotateCounterClockwise(rightDirection, i);
            Point expectedPosition = computeExpectedPosition(direction, timeStep);
            Point nextDirection = Point.getDirection(expectedPosition, target);
            double distance = Point.getDistance(expectedPosition, target);
            double speed = Simulator.getSpeed(nextDirection, this.windDirection);
            double timeToTarget = distance / speed;

            if (timeToTarget < minTimeToTarget) {
                minTimeToTargetDirection = direction;
                minTimeToTarget = timeToTarget;
            }
        }

        return minTimeToTargetDirection;
    }

    private Point computeExpectedPosition(Point moveDirection, double timeStep) {
        Point unitMoveDirection = Point.getUnitVector(moveDirection);
        double speed = Simulator.getSpeed(unitMoveDirection, this.windDirection);
        Point distanceMoved = new Point(
                unitMoveDirection.x * speed * timeStep,
                unitMoveDirection.y * speed * timeStep
        );
        Point nextLocation = Point.sum(this.currentLocation, distanceMoved);
        if (nextLocation.x < 0 || nextLocation.y > 10 || nextLocation.y < 0 || nextLocation.x > 10) {
            return this.currentLocation;
        }
        return nextLocation;
    }

    /**
    * visitedTargets.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> groupLocations, Map<Integer, Set<Integer>> visitedTargets) {
        // Let's use this information to prepare some convenient data structures:
        //  - For every player, unvisited targets, with a special variable pointing to ours.
        //  - For every target, players that have visited it. This will help compute potential score easily.
        this.visitedTargets = visitedTargets;

        for (Integer playerId : visitedTargets.keySet()) {
            Set<Integer> playerVisited = this.visitedTargets.get(playerId);
            Set<Integer> playerUnvisited = this.unvisitedTargets.get(playerId);
            playerUnvisited.removeAll(playerVisited);

            for (Integer target : playerVisited) {
                this.playerVisitsByTarget.get(target).add(playerId);
            }
        }
    }
}
