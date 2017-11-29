package sail.g1;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    private int K;

    int t;
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    int[] targetScore;
    Random gen;
    int id;
    Point wind_direction;
    Point currentLocation;
    Point initial;

    int curIndex;
    int[] nextTargetIndexes;
    int nextTargetIndex;

    int turn_num;

    boolean greedyChoice;

    double BESTANGLE = 0.511337; 
    double BESTANGLE_UPWIND = 0.68867265359;
    Point bestDirection1;
    Point bestDirection2;
    Point bestDirection1_upwind;
    Point bestDirection2_upwind;
    double timeOnBestDirection1;
    double timeOnBestDirection2;
    double timeSpent;
    boolean upwind;
    boolean lastMoveIs1 = true;

    int count;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        this.t = t;
        gen = new Random(seed);
        initial = initStartPoint(wind_direction, 0.0, 5.0, 5.0);
        this.wind_direction = wind_direction;
        double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    private Point initStartPoint(Point wind_direction, Double shift, Double xstart, Double ystart){
        Point origin = new Point(1, 0);
        Double angle = Point.angleBetweenVectors(origin, wind_direction);
        Double x, y;
        x = -shift * Math.cos(angle) + xstart;
        y = -shift * Math.sin(angle) + ystart;
        Point start = new Point(x, y);
        return start;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
        this.nextTargetIndex = -1;
        if(t <= 20) {
            this.K = Math.min(2, t);
        } else if(t <= 50) {
            this.K = Math.min(3, t);
        } else {
            this.K = Math.min(4, t);
        }
        targetScore = new int[targets.size()];
        for(int i = 0; i < targetScore.length; i++) {
            targetScore[i] = group_locations.size();
        }

        curIndex = -1;
        this.currentLocation = group_locations.get(id);
        initializeBestSpeedVector();
    }

    private void initializeBestSpeedVector(){
        double windAngle = Math.atan2(this.wind_direction.y, this.wind_direction.x);
        
        double speed = getSpeedRelativeToWind(BESTANGLE);
        this.bestDirection1 = new Point(speed * Math.cos(windAngle + BESTANGLE), speed * Math.sin(windAngle + BESTANGLE));
        this.bestDirection2 = new Point(speed * Math.cos(windAngle - BESTANGLE), speed * Math.sin(windAngle - BESTANGLE));
        
        speed = getSpeedRelativeToWind(Math.PI + BESTANGLE_UPWIND);
        this.bestDirection1_upwind = new Point(speed * Math.cos(windAngle + Math.PI + BESTANGLE_UPWIND), speed * Math.sin(windAngle + Math.PI + BESTANGLE_UPWIND));
        this.bestDirection2_upwind = new Point(speed * Math.cos(windAngle + Math.PI - BESTANGLE_UPWIND), speed * Math.sin(windAngle + Math.PI - BESTANGLE_UPWIND));
    }

    private double getSpeedRelativeToWind(double angle) {
        double x = 2.5 * Math.cos(angle + Math.PI) - 0.5;
        double y = 5 * Math.sin(angle + Math.PI);
        return Math.sqrt((x)*(x) + (y)*(y));
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        this.currentLocation = group_locations.get(id);
        return nearestNeighborMove(group_locations, id, dt, time_remaining_ms);
    }


    public Point nearestNeighborMove(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        turn_num++;
        boolean justHitATarget = false;
        while(curIndex < K && visited_set != null && visited_set.get(id).contains(nextTargetIndexes[curIndex])) {
            curIndex++;
            justHitATarget = true;
        }

        if(nextTargetIndexes == null || curIndex == K || curIndex == -1) {
            selectKBest(group_locations.get(id));
            curIndex = 0;
        }

        if(nextTargetIndexes[curIndex] == targets.size()) {
            return moveHelper(group_locations, initial, dt, justHitATarget);
        } else {
            return moveHelper(group_locations, targets.get(nextTargetIndexes[curIndex]), dt, justHitATarget);
        }
        /*
        if(nextTargetIndex == -1 || visited_set.get(id).contains(nextTargetIndex)) {
            return Point.getUnitVector(Point.getDirection(group_locations.get(id), selectKBest(group_locations.get(id))));
        } else {
            if(nextTargetIndex < targets.size()) {
                return Point.getUnitVector(Point.getDirection(group_locations.get(id), targets.get(nextTargetIndex)));
            } else {
                return Point.getUnitVector(Point.getDirection(group_locations.get(id), initial));
            }
        }
        */
    }

    private Point moveHelper(List<Point> group_locations, Point target, double dt, boolean justHitATarget) {
        if (dt <= 0.004) {
            if (justHitATarget) {
                Point direction = Point.getDirection(this.currentLocation, target);
                double x1 = group_locations.get(id).x;
                double y1 = group_locations.get(id).y;
                double x2 = target.x;
                double y2 = target.y;
                double d = Math.sqrt( (y2-y1)*(y2-y1) + (x2-x1)*(x2-x1) );
                double theta = Point.angleBetweenVectors(direction, wind_direction);
    
                double dx = d*Math.cos(theta);
                double dy = d*Math.sin(theta);
    
                if (theta <= BESTANGLE ||
                    theta >= -BESTANGLE + 2*Math.PI) {
                    timeOnBestDirection1 = (direction.y - bestDirection2.y/bestDirection2.x * direction.x)/(bestDirection1.y - bestDirection2.y/bestDirection2.x * bestDirection1.x);
                    timeOnBestDirection2 = (direction.y - bestDirection1.y/bestDirection1.x * direction.x)/(bestDirection2.y - bestDirection1.y/bestDirection1.x * bestDirection2.x);
                    this.upwind = false;
                } else if (theta >= Math.PI - BESTANGLE_UPWIND &&
                    theta <= Math.PI + BESTANGLE_UPWIND) {
                    timeOnBestDirection1 = (direction.y - bestDirection2_upwind.y/bestDirection2_upwind.x * direction.x)/(bestDirection1_upwind.y - bestDirection2_upwind.y/bestDirection2_upwind.x * bestDirection1_upwind.x);
                    timeOnBestDirection2 = (direction.y - bestDirection1_upwind.y/bestDirection1_upwind.x * direction.x)/(bestDirection2_upwind.y - bestDirection1_upwind.y/bestDirection1_upwind.x * bestDirection2_upwind.x);
                    this.upwind = true;
                } 
                if (upwind) {
                    return bestDirection1_upwind;
                }
                else {
                    return bestDirection1;
                }
            }
            else {
                if (timeOnBestDirection1 > dt && timeOnBestDirection2 > dt) {
                    return alternateBetween1And2(group_locations, dt);
                }
                else if (timeOnBestDirection1 > 2 * dt) {
                    timeOnBestDirection1 -= dt;
                    return this.upwind ? bestDirection1_upwind : bestDirection1;
                }
                else if (timeOnBestDirection2 > dt) {
                    timeOnBestDirection2 -= dt;
                    return this.upwind ? bestDirection2_upwind : bestDirection2;
                }
                else {
                    return computeNextDirection(target, dt);
                }
            }
        }
        else {
            return computeNextDirection(target, dt);
        }
    }

    private Point alternateBetween1And2(List<Point> group_locations, double dt) {
        this.currentLocation = group_locations.get(id);
        Point move;
        Point newLocation = new Point(bestDirection1.x * dt + group_locations.get(id).x, bestDirection1.y * dt + group_locations.get(id).y);
        Point newLocation2 = new Point(bestDirection2.x * dt + group_locations.get(id).x, bestDirection2.y * dt + group_locations.get(id).y);

        if (lastMoveIs1 && upwind) {
            timeOnBestDirection2 -= dt;
            move = bestDirection2_upwind;
        }
        else if (lastMoveIs1 && !upwind) {
            timeOnBestDirection2 -= dt;
            move = bestDirection2;
        }
        else if (!lastMoveIs1 && upwind) {
            timeOnBestDirection1 -= dt;
            move = bestDirection1_upwind;
        }
        else {
            timeOnBestDirection1 -= dt;
            move = bestDirection1;
        }
        lastMoveIs1 = !lastMoveIs1;
        return move;
    }

    private Point computeNextDirection(Point target, double dt) {
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        Point perpendicularLeftDirection = Point.rotateCounterClockwise(directionToTarget, Math.PI/2.0);
        Point perpendicularRightDirection = Point.rotateCounterClockwise(directionToTarget, -Math.PI/2.0);
        return findBestDirection(perpendicularLeftDirection, perpendicularRightDirection, target, 100, dt);
    }

    private Point findBestDirection(Point leftDirection, Point rightDirection, Point target, int numSteps,
                                    double dt) {
        // First, check if the target is reachable in one time step from our current position.
        double currentDistanceToTarget = Point.getDistance(this.currentLocation, target);
        Point directionToTarget = Point.getDirection(this.currentLocation, target);
        double speedToTarget = Simulator.getSpeed(directionToTarget, this.wind_direction);
        double distanceWeCanTraverse = speedToTarget * dt;
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
            Point expectedPosition = computeExpectedPosition(direction, dt);
            Point nextDirection = Point.getDirection(expectedPosition, target);
            double distance = Point.getDistance(expectedPosition, target);
            double speed = Simulator.getSpeed(nextDirection, this.wind_direction);
            double timeToTarget = distance / speed;

            if (timeToTarget < minTimeToTarget) {
                minTimeToTargetDirection = direction;
                minTimeToTarget = timeToTarget;
            }
        }

        return minTimeToTargetDirection;
    }

    private Point computeExpectedPosition(Point moveDirection, double dt) {
        Point unitMoveDirection = Point.getUnitVector(moveDirection);
        double speed = Simulator.getSpeed(unitMoveDirection, this.wind_direction);
        Point distanceMoved = new Point(
                unitMoveDirection.x * speed * dt,
                unitMoveDirection.y * speed * dt
        );
        Point nextLocation = Point.sum(this.currentLocation, distanceMoved);
        if (nextLocation.x < 0 || nextLocation.y > 10 || nextLocation.y < 0 || nextLocation.x > 10) {
            return this.currentLocation;
        }
        return nextLocation;
    }

    public int[] selectKBest(Point curLoc) {
        double maxScore = -1;
        int targetIndex = -1;
        int[] p = new int[K];

        int nearestTarget = findNearestNeighbor(curLoc);
        if(visited_set != null && visited_set.get(this.id).size() > targets.size() - K) {
            nextTargetIndex = nearestTarget;
            nextTargetIndexes[0] = nearestTarget;
            return nextTargetIndexes;
        }

        //maxScore = getTargetScore(curLoc, nearestTarget);
        //targetIndex = nearestTarget;
        for(int i = 0; i < targets.size(); i++) {
            if(visited_set != null && visited_set.get(this.id).contains(i)) continue;

            p = findNearestNeighbors(i);

            // Compute time to get to target
            double score = getTargetsScore(curLoc, p);
            if(score > maxScore) {
                maxScore = score;
                targetIndex = i;
                nextTargetIndexes = p;
            }
        }


        if(targetIndex == -1) {
            targetIndex = targets.size();
        }

        nextTargetIndex = targetIndex;


        return nextTargetIndexes;
    }


    public int[] findNearestNeighbors(int targetIndex) {
        int[] nextPoints = new int[K];
        nextPoints[0] = targetIndex;
        Set<Integer> excluded = new HashSet<Integer>();
        excluded.add(nextPoints[0]);
        for(int i = 1; i < nextPoints.length; i++) {
            nextPoints[i] = findNearestTarget(nextPoints[i-1], excluded);
            excluded.add(nextPoints[i]);
        }

        return nextPoints;
    }


     private int findNearestTarget(int targetIndex, Set<Integer> excluded) {
        int nearestTargetIndex = -1;
        double maxScore = -1;
        Point targetLoc = targets.get(targetIndex);
        for(int i = 0; i < targets.size(); i++) {
            if(excluded.contains(i) || visited_set != null && visited_set.get(this.id).contains(i)) continue;

            // Compute time to get to target
            double score = getTargetScore(targetLoc, i);
            if(score > maxScore) {
                maxScore = score;
                nearestTargetIndex = i;
            }
        }

        // No more neighbors so return to starting point
        if(nearestTargetIndex == -1) {
            nearestTargetIndex = targets.size();
        }

        return nearestTargetIndex;
    }



    private int findNearestNeighbor(Point curLoc) {
        Point nearestTarget = null;
        int targetIndex = -1;
        double maxScore = -1;
        for(int i = 0; i < targets.size(); i++) {
            if(visited_set != null && visited_set.get(this.id).contains(i)) continue;

            // Compute time to get to target
            double score = getTargetScore(curLoc, i);

            /*
            if(nearestTarget != null && i != nextTargetIndex && Point.getDistance(targets.get(nextTargetIndex), targets.get(i)) <= 0.02) {
                nearestTarget = new Point((targets.get(i).x + targets.get(nextTargetIndex).x)/2, (targets.get(i).y +  targets.get(nextTargetIndex).y)/2);
                nextTargetIndex = i;
                minTime = getTimeToTravel(curLoc, nearestTarget);
            } else
            */


            if(score > maxScore) {
                nearestTarget = targets.get(i);
                maxScore = score;
                targetIndex = i;
            }
        }

        // No more neighbors so return to starting point
        if(nearestTarget == null) {
            nearestTarget = initial;
            targetIndex = targets.size();
        }

        return targetIndex;
    }

    private double getTimeToTravel(Point from, Point to) {
        return Point.getDistance(from, to)/Simulator.getSpeed(Point.getDirection(from, to), wind_direction);
    }

    private double getTargetScore(Point from, int targetIndex) {
        double travelTime = getTimeToTravel(from, targets.get(targetIndex));
        return targetScore[targetIndex]/(travelTime);
    }

    private double getTargetsScore(Point from, int[] targetIndexes) {
        double travelTime = getTimeToTravel(from, targets.get(targetIndexes[0]));
        double score = targetScore[targetIndexes[0]];
        for(int i = 1; i < targetIndexes.length; i++) {
            travelTime += getTimeToTravel(targets.get(targetIndexes[i-1]), targets.get(targetIndexes[i]));
            score += targetScore[targetIndexes[i]];
        }

        return score/(travelTime);
    }


    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
        for(int i = 0; i < targetScore.length; i++) {
            targetScore[i] = group_locations.size();
        }

        for(int k : visited_set.keySet()) {
            Set<Integer> visitedTargets = visited_set.get(k);
            for(int l : visitedTargets) {
                targetScore[l]--;
            }
        }
    }
}
