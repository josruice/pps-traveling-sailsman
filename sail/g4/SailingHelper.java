package sail.g4x;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.*;

public class SailingHelper {

	public final double WINDSPEED = 50.0;
	List<Point> targets;
    int id;
    Point initial;
    Point wind_direction;
    double wind_angle;

    public SailingHelper(List<Point> targets, int id, Point initial, Point wind_direction, double wind_angle)	{
    	this.targets = targets;
    	this.id = id;
    	this.initial = initial;
    	this.wind_direction = wind_direction;
    	this.wind_angle = wind_angle;
    }

    public int valueOf(int targetIndex, HashMap<Integer, Set<Integer>> visited_set) {
        return -1;
    }

    public double squared(double x) {
        return x*x;
    }

    public double getPathDistance(Point currentLoc, ArrayList<Point> path) {
        double ret = Point.getDistance(currentLoc, path.get(0));
        for (int i = 1; i < path.size(); i++) {
            ret += Point.getDistance(path.get(i-1), path.get(i));
        }
        return ret;
    }

    public double getPathDistanceByTime(Point currentLoc, ArrayList<Point> path) {
        double ret = Point.getDistance(currentLoc, path.get(0))/Simulator.getSpeed(Point.getDirection(currentLoc, path.get(0)), wind_direction);
        for (int i = 1; i < path.size(); i++) {
            ret += Point.getDistance(path.get(i-1), path.get(i))/Simulator.getSpeed(Point.getDirection(path.get(i-1), path.get(i)), wind_direction);
        }
        return ret;
    }

    public boolean closeToBoundary(Point p, double dt) {
        // this is a temporary way to tell if it's okay to curve
        // taking into account the current [bug](removed) units inconsistency in the simulator
        double maxOrthogonalDistanceOfMove = 1.0 * 5.0 * dt;
        return p.x < maxOrthogonalDistanceOfMove || p.y < maxOrthogonalDistanceOfMove || 10.0-p.x < maxOrthogonalDistanceOfMove || 10.0-p.y < maxOrthogonalDistanceOfMove;

    }
    // foud on internet
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

    public ArrayList<Point> findBestPathByTime(Point currentLoc, ArrayList<Point> points) {
        int ps = points.size();
        //System.out.println(points.size());
        double lowestDist = Double.MAX_VALUE;
        double d;
        ArrayList<Point> bestPathSoFar = null;
        for (ArrayList<Point> path : listPermutations(points)) {
            if (path.size() == ps) {
                d = getPathDistanceByTime(currentLoc, path);
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

    public Point getClosestTargetByTime(ArrayList<Integer> availableTargetIndices, Point currentLoc) {
        Point closest = null;
        double d;
        double minDist = 10000000;
        for (int i : availableTargetIndices) {
            d = Point.getDistance(currentLoc, targets.get(i))/Simulator.getSpeed(Point.getDirection(currentLoc, targets.get(i)), wind_direction);
            if (d < minDist) {
                minDist = d;
                closest = targets.get(i);
            }
        }
        return closest;
    }

    public ArrayList<Point> getkOptimalTargets(ArrayList<Integer> availableTargetIndices, Point currentLoc, int k) {
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
        return findBestPathByTime(currentLoc, kClosest)/*.get(0)*/;
    }

}