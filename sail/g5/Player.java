package sail.g5;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    Map<Integer, Set<Integer>> visitedTargets;
    Random gen;
    int id;
    int numTargets;
    Point initialLocation;
    Point currentLocation;
    Point windDirection;

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
    }

    @Override
    public Point move(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs) {
        this.currentLocation = groupLocations.get(id);
        return greedyMove(groupLocations, id, timeStep, timeRemainingMs);
    }

    // Applies the Greedy Strategy to Find the next Point
    public Point greedyMove(List<Point> groupLocations, int id, double timeStep, long timeRemainingMs){
        Point myCurrentLocation = groupLocations.get(id);
        int nTargets = targets.size();
        Set<Integer> unVisitedTargets = new HashSet<Integer>();
        Point nextTarget = initialLocation; // If no unvisited targets, initial location will be out next target.

        for (int i = 0; i < nTargets; i ++){
            if (visitedTargets == null || !visitedTargets.get(id).contains(i))
                unVisitedTargets.add(i);
        }

        double minTime = Double.MAX_VALUE;
        for (int unvisitedTargetPoint : unVisitedTargets){
            double distance = Point.getDistance(myCurrentLocation, (Point) targets.get(unvisitedTargetPoint));
            Point direction = Point.getDirection(myCurrentLocation, (Point) targets.get(unvisitedTargetPoint));
            double speed = Simulator.getSpeed(direction, windDirection);
            double time = distance/speed;

            if (time < minTime){
                minTime = time;
                nextTarget = targets.get(unvisitedTargetPoint);
            }
        }

        return computeNextDirection(nextTarget, timeStep);
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
        this.visitedTargets = visitedTargets;
    }
}
