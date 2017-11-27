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
    Point initialLocation;
    Point currentLocation;
    Point windDirection;
    final String STRATEGY = "rewardBased"; // One of: greedy, weightedGreedy

    @Override
    public Point chooseStartingLocation(Point windDirection, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        
        // RANDOMLY GENERATED INITIAL POINT
        // initialLocation = new Point(gen.nextDouble()*10, gen.nextDouble()*10); 
        
        
        // CENTRE POINT AS THE CENTRE 
        //initialLocation = new Point(5.0, 5.0); 

        /* 
            We consider the four corners of the grid with a randomly generated epsilon as the deviation
            The speed wrt the wind direction is computed at all four locations
            The one which points to the fastest speed is considered as the initial point

            The logic behind that is that, the player will get an upper hand by having an accelerated motion  
        */

        List<Point> possibleStartPoints = new ArrayList<Point>();  // List of four points with possible deviations from the corners
        double epsilon = 1 + 3 * gen.nextDouble(); // Epsilon is considered as a randomly generated value from [1, 3]
        possibleStartPoints.add(new Point(0 + epsilon, 0 + epsilon));
        possibleStartPoints.add(new Point(0 + epsilon, 10 - epsilon));
        possibleStartPoints.add(new Point(10 - epsilon, 0 + epsilon));
        possibleStartPoints.add(new Point(10 - epsilon, 10 - epsilon));

        double minSpeed = Double.MAX_VALUE;
        Point optimalPoint = new Point(5.0, 5.0); // By default the center is the optimum point 

        //The speed wrt to all points in the list is considered. One with the maximum speed is the optimal point
        for (Point p: possibleStartPoints){
            if (Simulator.getSpeed(p, windDirection) < minSpeed)
                optimalPoint = p;
        }
        initialLocation = optimalPoint;
        this.numTargets = t; 
        this.windDirection = windDirection;
        return initialLocation;
    }

    @Override
    public void init(List<Point> groupLocations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.numPlayers = groupLocations.size();

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
    }

    @Override
    public Point move(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs) {
        this.currentLocation = groupLocations.get(id);

        // If all the target locations are visited, then return back to the initial position
        if (ourUnvisitedTargets.size() == 0)
            return computeNextDirection(initialLocation, timeStep);

        switch (STRATEGY) {
            case "greedy":
                return greedyMove(groupLocations, id, timeStep, timeRemainingMs);
            case "weightedGreedy":
                return weightedGreedyMove(groupLocations, id, timeStep, timeRemainingMs);
            case "weightedGreedy2":
                return weightedGreedyMove2(groupLocations, id, timeStep, timeRemainingMs);
            case "rewardBased":
                return rewardBasedMove(groupLocations, id, timeStep, timeRemainingMs);

            default:
                System.err.println("Invalid strategy "+STRATEGY+" chosen");
                return new Point(0,0);
        }
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

    // Applies a weighted greedy based move based on a newly defined heuristic
    public Point weightedGreedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        // Let's get the maximum weight unvisited target according to the following formula:
        //  targetWeight = (targetRemainingScore * distanceFromNonVisitedPlayers) / timeToTarget;

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : this.ourUnvisitedTargets) {
            double ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
            int score = computeRemainingScore(targetId);
            
            double othersTime = computeUnvisitedPlayersTimeTo(groupLocations, targetId);
             
            double weight = (score * othersTime) / ourTime;
            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }

        return computeNextDirection(maxWeightTarget, timeStep);
    }

    // Modifies the existing weighted greedy based move based on other's players travel time 
    public Point weightedGreedyMove2(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        // Let's get the maximum weight unvisited target according to the following formula:
        // targetWeight = (targetRemainingScore * distanceFromNonVisitedPlayers) / timeToTarget;
        // Our distance is updated based on that of other's distance
        // Rather than using the total distance from non-viisted players, the minimum distance is considered 

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : this.ourUnvisitedTargets) {
            double ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
            int score = computeRemainingScore(targetId);
            
            double othersTime = minComputeUnvisitedPlayersTimeTo(groupLocations, targetId); // Rather than considering the distance, other player's minimum most distance is considered 
            double lambda = 1.1; // Empirically found value of lambda to adjust our time 

            // If any other player is closed to the target than us, then we update our time by increasing it by lambda times
            if (score > 5 && ourTime > othersTime)
                ourTime = 1.1 * ourTime;
                 
            double weight = (score * othersTime) / ourTime;
            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }

        return computeNextDirection(maxWeightTarget, timeStep);
    }

    // Chooses the nearest target point with a reward given to the point with more nearer neighboring points 
    public Point rewardBasedMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        // On top of the existing weighted greedy mechanism, a discounted reward is used for those target points which have many nearer neighboring points

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;  // If no unvisited targets, initial location will be our next target.
        for (int targetId : this.ourUnvisitedTargets) {
            double ourTime = computeEstimatedTimeToTarget(targets.get(targetId));
            int score = computeRemainingScore(targetId);
            
            double othersTime = minComputeUnvisitedPlayersTimeTo(groupLocations, targetId);
            
            if (score > 5 && ourTime > othersTime)
                ourTime = 1.1 * ourTime;
            
            double discountFactor = 0.5; 
            
            Point targetId2 = findNearestPoint(targets.get(targetId));
            double distance2 = getNearestDistance(targets.get(targetId));

            double weight = (score * othersTime) / ourTime + discountFactor/distance2;

            if (weight > maxWeight) {
                maxWeight = weight;
                maxWeightTarget = targets.get(targetId);
            }
        }

        return computeNextDirection(maxWeightTarget, timeStep);
    } 


    // Finds the nearest target point to the current location
    public Point findNearestPoint(Point myLocation){
	    double minDistance = Double.MAX_VALUE;
	    Point optimalPoint = targets.get(0);

	    for(int consideredPointIdx : ourUnvisitedTargets){            
            Point consideredPoint = targets.get(consideredPointIdx);
            if(consideredPoint == myLocation)
                continue;
		    double distance = computeEstimatedTimeToTarget(myLocation, consideredPoint);
		    if (distance < minDistance){
			    minDistance = distance;
			    optimalPoint = consideredPoint; 
		    }
	    }

	    return optimalPoint;
    }

    // Finds the distance to the nearest target point from the current location
    public double getNearestDistance(Point myLocation){
        if(ourUnvisitedTargets.size() < 3)
            return 0.0;

	    Point nearestPoint = findNearestPoint(myLocation);
	    return computeEstimatedTimeToTarget(myLocation, nearestPoint);
    }

    // Computes the total distance of all the unvisited players from the given target location
    private double computeUnvisitedPlayersTimeTo(List<Point> groupLocations, int targetId) {
        double distance = 0.0;
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


    // Computes the minimum time taken to travel to the given target location amongst all the unvisited players
    private double minComputeUnvisitedPlayersTimeTo(List<Point> groupLocations, int targetId) {
        double minDistance = Double.MAX_VALUE;
        double distance = 0.0;
        Point target = this.targets.get(targetId);
        for (int playerId = 0; playerId < this.numPlayers; ++playerId) {
            if (playerId == this.id) continue; // Skip our own.
            if (!this.playerVisitsByTarget.get(targetId).contains(playerId)) {
                // This means that this player hasn't visited this target yet, so
                // compute her time to target.
                Point player = groupLocations.get(playerId);
                distance = computeEstimatedTimeToTarget(player, target);

                if (distance < minDistance)
                    minDistance = distance;
 
            }
        }
        return minDistance;
    }


    // Given a target location, computes the number of players yet to visit it 
    private int computeRemainingScore(int targetId) {
        return this.numPlayers - this.playerVisitsByTarget.get(targetId).size();
    }


    // Computes the estimated time taken to reach the target location from the current location
    private double computeEstimatedTimeToTarget(Point target) {
        return computeEstimatedTimeToTarget(this.currentLocation, target);
    }


    // Computes the estimated time taken by the given player to reach a given location
    private double computeEstimatedTimeToTarget(Point player, Point target) {
        double distance = Point.getDistance(player, target);
        Point direction = Point.getDirection(player, target);
        double speed = Simulator.getSpeed(direction, windDirection);
        return distance/speed;
    }

    // Given a target and timestep, finds the direction to reach the target
    private Point computeNextDirection(Point target, double timeStep) {
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        Point perpendicularLeftDirection = Point.rotateCounterClockwise(directionToTarget, Math.PI/2.0);
        Point perpendicularRightDirection = Point.rotateCounterClockwise(directionToTarget, -Math.PI/2.0);
        return findBestDirection(perpendicularLeftDirection, perpendicularRightDirection, target, 100, timeStep);
    }

    // Given a target, number of steps and the timestep, finds the best possible direction to reach the target
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

    // Computes the next expected position when followed the direction and the time step
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
