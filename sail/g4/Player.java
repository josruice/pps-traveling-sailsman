package sail.g4;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    public final double WINDSPEED = 50.0;
    List<Point> targets;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    Point wind_direction;
    double wind_angle;
    Point currentTarget = null;
    boolean inStraightMode = false;

    public double squared(double x) {
        return x*x;
    }

    public double getPathDistance(Point currentLoc, ArrayList<Point> points) {
        double ret = Point.getDistance(currentLoc, points.get(0));
        for (int i = 1; i < points.size(); i++) {
            ret += Point.getDistance(points.get(i-1), points.get(i));
        }
        return ret;
    }

    public ArrayList<ArrayList<Point>> listPermutations(ArrayList<Point> list) {

        if (list.size() == 0) {
            ArrayList<ArrayList<Point>> result = new ArrayList<ArrayList<Point>>();
            result.add(new ArrayList<Point>());
            return result;
        }
        ArrayList<ArrayList<Point>> returnMe = new ArrayList<ArrayList<Point>>();
        Point firstElement = list.remove(0);
        ArrayList<ArrayList<Point>> recursiveReturn = listPermutations(list);
        for (ArrayList<Point> li : recursiveReturn) {

            for (int index = 0; index <= li.size(); index++) {
                ArrayList<Point> temp = new ArrayList<Point>(li);
                temp.add(index, firstElement);
                returnMe.add(temp);
            }
        }
        return returnMe;
}

    public ArrayList<Point> findBestPath(Point currentLoc, ArrayList<Point> points) {
        int ps = points.size();
        //System.out.println(points.size());
        double lowestDist = Double.MAX_VALUE;
        double d;
        ArrayList<Point> bestPathSoFar = null;
        for (ArrayList<Point> path : listPermutations(points)) {
            if (path.size() == ps) {
                d = getPathDistance(currentLoc, path);
                //System.out.println(d);
                if (d < lowestDist) {
                    lowestDist = d;
                    bestPathSoFar = path;
                }
            }
        }
        return bestPathSoFar;
    }

    public boolean isInsideBox(Point p) {
        return (0.0 <= p.x) && (p.x <= 10.0) && (0.0 <= p.y) && (p.y <= 10.0);
    }

    public double bringIntoRange(double angle) {
        if (angle < 0)          return bringIntoRange(angle + 2*Math.PI);
        if (angle > 2* Math.PI) return bringIntoRange(angle - 2*Math.PI);

        return angle;
    }

    public double utilFunction(double phi, double theta) {
        // you need to go towards a point in the phi direction from where you are
        // this computes a utility of going in a direction theta
        // we will be maximizing this function using gradient ascent
        return Math.cos(phi - theta) * Math.sqrt(6.5 - 2.5*Math.cos(theta) + 18.75*squared(Math.sin(theta)));
    }

    public double dUtildTheta(double phi, double theta) {
        double small = .00000001;
        return (utilFunction(phi, theta+small) - utilFunction(phi, theta))/small;
    }

    public double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(squared(x2-x1) + squared(y2-y1));
    }

    public Point getUnitVector(double angle) {
        return new Point(Math.cos(angle), Math.sin(angle));
    }

    public double getRelAngle(Point p1, Point p2) {
        double xDiff = p2.x - p1.x;
        double yDiff = p2.y - p1.y;
        return bringIntoRange(Math.atan2(yDiff, xDiff) - wind_angle + Math.PI);
    }

    public boolean shouldCurve(double angleGiven) {
        double angle = bringIntoRange(angleGiven);
        if (0 <= angle && angle <= .5) {
            return true;
        }
        if (5.75 <= angle && angle <= 2*Math.PI) {
            return true;
        }
        if (2.8 <= angle && angle <= 3.5) {
            return true;
        }
        return false;
    }

    public Point getBestAbsoluteDirection(Point p1, Point p2) {
        double xDiff = p2.x - p1.x;
        double yDiff = p2.y - p1.y;
        return getUnitVector(getBestTheta(Math.atan2(yDiff, xDiff)));
    }

    public double getBestAbsoluteAngle(Point p1, Point p2) {
        double xDiff = p2.x - p1.x;
        double yDiff = p2.y - p1.y;
        return getBestTheta(Math.atan2(yDiff, xDiff));
    }

    public double getAbsoluteAngle(Point p1, Point p2) {
        double xDiff = p2.x - p1.x;
        double yDiff = p2.y - p1.y;
        return Math.atan2(yDiff, xDiff);
    }

    public double getBestTheta(double phi_abs) {
        // find phi, which is the angle we seek to go in, but relative to the wind
        // will convert back at end of function
        // relative_angle = absolute_angle - wind_angle + PI
        double phi = phi_abs - wind_angle + Math.PI;
        // given a phi constant, find the theta that maximizes utilFunction
        int numIterations = 100;
        if (phi == 0.0) return .689; // special case
        double firstTry;
        // use gradient ascent
        double eta = .01; // learning rate
        double currentTheta = 2.0;
        double newTheta;
        for (int i = 0; i < numIterations; i++) {
            newTheta = bringIntoRange(currentTheta + eta*dUtildTheta(phi, currentTheta));
            if (utilFunction(phi, newTheta) > utilFunction(phi, currentTheta)) {
                currentTheta = newTheta;
            } else {
                eta *= .99;
            }
        }
        firstTry = currentTheta;

        double secondTry;
        eta = .01; // learning rate
        currentTheta = 4.0;
        for (int i = 0; i < numIterations; i++) {
            newTheta = bringIntoRange(currentTheta + eta*dUtildTheta(phi, currentTheta));
            if (utilFunction(phi, newTheta) > utilFunction(phi, currentTheta)) {
                currentTheta = newTheta;
            } else {
                eta *= .99;
            }
        }
        secondTry = currentTheta;
        // when returning, convert back to nonrelative angle
        if (utilFunction(phi, firstTry) > utilFunction(phi, secondTry)) return firstTry + wind_angle - Math.PI;
        return secondTry + wind_angle - Math.PI;
    }

    public double getSpeed(double theta) {
        // takes in the relative angle you're going at
        return Math.sqrt(6.5 - 2.5*Math.cos(theta) + 18.75*squared(Math.sin(theta))) * WINDSPEED;
    }



    public double timeTakenGreedy(double distance, double phi, double dt, double howCloseNeedToBe) {
        // time taken to travel in the phi-direction for a given distance
        double timeElapsed = 0.0;
        double bestTheta;
        double xDiff, yDiff;
        double dirToGoal, dirNow, speedNow, distanceLeft;
        double relativeCurrentX = 0.0;
        double relativeCurrentY = 0.0;
        double relativeGoalX = distance*Math.cos(phi);
        double relativeGoalY = distance*Math.sin(phi);
        ArrayList<Double> xs = new ArrayList<Double>();
        ArrayList<Double> ys = new ArrayList<Double>();
        int count = 0;
        while (Math.sqrt(squared(relativeGoalX-relativeCurrentX) + squared(relativeGoalY-relativeCurrentY)) > howCloseNeedToBe) {
        //for (int i = 0; i < 400; i++) {
            xDiff = relativeGoalX - relativeCurrentX;
            yDiff = relativeGoalY - relativeCurrentY;
            dirToGoal = Math.atan2(yDiff, xDiff);
            //System.out.println("direction to goal: " + dirToGoal);
            dirNow = getBestTheta(dirToGoal);
            speedNow = getSpeed(dirNow);
            relativeCurrentX += dt * speedNow * Math.cos(dirNow);
            relativeCurrentY += dt * speedNow * Math.sin(dirNow);
            timeElapsed += dt;
            distanceLeft = dist(relativeCurrentX, relativeCurrentY, relativeGoalX, relativeGoalY);
            count++;

        }
        return timeElapsed;
    }

    public double timeTakenStraightIncremental(double distance, double phi, double dt, double howCloseNeedToBe) {
        // time taken to travel in the phi-direction for a given distance
        double timeElapsed = 0.0;
        double speedNow;
        double distanceLeft;
        double relativeCurrentX = 0.0;
        double relativeCurrentY = 0.0;
        double relativeGoalX = distance*Math.cos(phi);
        double relativeGoalY = distance*Math.sin(phi);
        while (Math.sqrt(squared(relativeGoalX-relativeCurrentX) + squared(relativeGoalY-relativeCurrentY)) > howCloseNeedToBe) {
        //for (int i = 0; i < 400; i++) {
            speedNow = getSpeed(phi);
            relativeCurrentX += dt * speedNow * Math.cos(phi);
            relativeCurrentY += dt * speedNow * Math.sin(phi);
            timeElapsed += dt;
            distanceLeft = dist(relativeCurrentX, relativeCurrentY, relativeGoalX, relativeGoalY);
            // xs.add(relativeCurrentX);
            // ys.add(relativeCurrentY);

        }
        //System.out.println("Goal was: (" + relativeGoalX + ", " + relativeGoalY + ")");
        //drawPoints(xs, ys);
        return timeElapsed;
    }

    public Point getClosestTarget(ArrayList<Integer> availableTargetIndices, Point currentLoc){
        Point closest = null;
        double d;
        double minDist = 50*Math.sqrt(2);
        for (int i : availableTargetIndices) {
            d = Point.getDistance(currentLoc, targets.get(i));
            if (d < minDist) {
                minDist = d;
                closest = targets.get(i);
            }
        }
        return closest;
    }

    public Point getkOptimalTarget(ArrayList<Integer> availableTargetIndices, Point currentLoc){
        int k = 5;
        ArrayList<Point> availableTargets = new ArrayList<Point>();
        for (int i : availableTargetIndices) {
            availableTargets.add(targets.get(i));
        }
        Collections.sort(availableTargets, 
                        (o1, o2) -> (Point.getDistance(currentLoc, o1) < (Point.getDistance(currentLoc, o2))) ? -1 : 1);
        ArrayList<Point> kClosest = new ArrayList<Point>();
        for (int i = 0; i < Math.min(k, availableTargets.size()); i++) {
            kClosest.add(availableTargets.get(i));
        }
        for (Point p : kClosest) {
            System.out.println(p.x + ", "+ p.y + " -- #" + targets.indexOf(p));
        }
        return findBestPath(currentLoc, kClosest).get(0);
    }

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        this.wind_direction = wind_direction;
        this.wind_angle = Math.atan2(wind_direction.y, wind_direction.x);
        gen = new Random(seed);
        initial = new Point(5.0, 5.0);//new Point(gen.nextDouble()*10, gen.nextDouble()*10);
        double speed = Simulator.getSpeed(initial, wind_direction);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
    }

    public boolean closeToBoundary(Point p, double dt) {
        // this is a temporary way to tell if it's okay to curve
        // taking into account the current bug in the simulator
        double maxDistanceOfMove = 1.0 * 5.0 * dt;
        return p.x < maxDistanceOfMove || p.y < maxDistanceOfMove || 10.0-p.x < maxDistanceOfMove || 10.0-p.y < maxDistanceOfMove;

    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {
        // testing timeouts... 
        // try {
        //     TimeUnit.MILLISECONDS.sleep(1);
        // } catch(Exception ex) {
        //     ;
        // }
        // just for first turn
        //System.out.println(Simulator.getSpeed(new Point(.5, .5), new Point(-.5, -.5)));
        Point currentLoc = group_locations.get(id);
        //System.out.println(currentLoc.x + ", " + currentLoc.y);
        Point goalLoc;
        ArrayList<Integer> availableTargetIndices = new ArrayList<Integer>();
        // short-circuiting is great
        if(visited_set != null && visited_set.get(id).size() == targets.size()) {
            //this is if finished visiting all
            goalLoc = initial;
        } else {
            for(int i = 0; i < targets.size(); i++) {
                // short-circuiting remains great
                if (visited_set == null || !visited_set.get(id).contains(i)) {
                    availableTargetIndices.add(i);
                }
            }
            
            // either A:
            goalLoc = getClosestTarget(availableTargetIndices, currentLoc);
            // \end A.
            
            // or B:
            // if (currentTarget != null && availableTargetIndices.contains(targets.indexOf(currentTarget))) {
            //     System.out.println("already have current target");
            //     goalLoc = currentTarget;
            // } else {
            //     System.out.println("AVAILABLE TARGET INDICES:");
            //     for (Integer i : availableTargetIndices) {
            //         System.out.print(i + " ");
            //     }
            //     System.out.println();
            //     goalLoc = getkOptimalTarget(availableTargetIndices, currentLoc);
            //     currentTarget = goalLoc;
            //     System.out.println("new target is " + currentTarget.x + ", " + currentTarget.y);
            // }
            // \end B.

        }
        double relAngleWouldNeedToGoIn = getRelAngle(currentLoc, goalLoc);
        // double actualAngleWouldNeedToGoIn = getAbsoluteAngle(currentLoc, goalLoc);
        // double dx = dt * getSpeed(getBestTheta(actualAngleWouldNeedToGoIn)-wind_angle+Math.PI) * Math.cos(getBestAbsoluteAngle(currentLoc, goalLoc));
        // double dy = dt * getSpeed(getBestTheta(actualAngleWouldNeedToGoIn)-wind_angle+Math.PI) * Math.sin(getBestAbsoluteAngle(currentLoc, goalLoc));
        // Point newPointCurvingWouldGive = Point.sum(currentLoc, new Point(dx, dy));
        // if (shouldCurve(relAngleWouldNeedToGoIn) && !isInsideBox(newPointCurvingWouldGive)) {
        //     System.out.println("prevented colliding with border. was at "+ currentLoc.x + ", " + currentLoc.y + " and would have ended up at " +newPointCurvingWouldGive.x + ", " + newPointCurvingWouldGive.y);
        // }
        if (shouldCurve(relAngleWouldNeedToGoIn) && !closeToBoundary(currentLoc, dt)/*&& isInsideBox(newPointCurvingWouldGive)*/) {
            return getBestAbsoluteDirection(currentLoc, goalLoc);
        } else {
            return Point.getDirection(currentLoc, goalLoc);
        }
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
