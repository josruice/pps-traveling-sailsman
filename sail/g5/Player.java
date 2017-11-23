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
    final String STRATEGY = "greedy"; // One of: greedy, weightedGreedy

    @Override
    public Point chooseStartingLocation(Point windDirection, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        gen = new Random(seed);
        initialLocation = new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        double speed = Simulator.getSpeed(initialLocation, windDirection);
        this.numTargets = t;
        this.windDirection = windDirection;
        // initialLocation = new Point(5.0, 5.0); // Use the middle point as initial location.
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
        switch (STRATEGY) {
            case "greedy":
                return greedyMove(groupLocations, id, timeStep, timeRemainingMs);
            case "weightedGreedy":
                return weightedGreedyMove(groupLocations, id, timeStep, timeRemainingMs);
            default:
                System.err.println("Invalid strategy "+STRATEGY+" chosen");
                return new Point(0,0);
        }
    }

    public Point weightedGreedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        Point nextTarget = initialLocation; // If no unvisited targets, initial location will be our next target.

        // Let's get the maximum weight unvisited target according to the following formula:
        //  targetWeight = targetRemaningScore / timeToTarget;

        double maxWeight = Double.NEGATIVE_INFINITY;
        Point maxWeightTarget = this.initialLocation;
        for (int targetId : this.ourUnvisitedTargets) {
            double time = computeEstimatedTimeToTarget(targets.get(targetId));
            int score = computeRemainingScore(targetId);
            double weight = score / time;
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

    private int computeRemainingScore(int targetId) {
        return this.numPlayers - this.playerVisitsByTarget.get(targetId).size();
    }

    private double computeEstimatedTimeToTarget(Point target) {
        double distance = Point.getDistance(this.currentLocation, target);
        Point direction = Point.getDirection(this.currentLocation, target);
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
