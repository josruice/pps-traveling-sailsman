package sail.g4;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.*;



public class SailingHelper {

public class TargetStats {

    public int targetIndex;
    public double timeToTarget;
    public double distToTarget;
    public double heuristic;
}

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
    
    public double squared(double x) {
        return x*x;
    }

    public boolean closeToBoundary(Point p, double dt) {
        double maxOrthogonalDistanceOfMove = 1.0 * 5.0 * dt;
        return p.x < maxOrthogonalDistanceOfMove || p.y < maxOrthogonalDistanceOfMove || 10.0-p.x < maxOrthogonalDistanceOfMove || 10.0-p.y < maxOrthogonalDistanceOfMove;
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

    public Point getClosestTarget(ArrayList<Integer> availableTargetIndices, Point currentLoc)  {
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

    public TargetStats getHeuristicDistance(ArrayList<Integer> availableTargetIndices, Point currentLoc, 
        List<Point> groupLocations, Map<Integer, Set<Integer>> visited_set, int id) {

        TargetStats targetStats = new TargetStats();
        double maxTargetValue = 0.0;
        int numPlayers = groupLocations.size();
        int maxiIndex = availableTargetIndices.get(0);

        for (int targetIndex : availableTargetIndices) {
                Point target = targets.get(targetIndex);

                double targetRealValue = numPlayers;
                double targerProjectedValue = numPlayers;

                for (int p = 0; p < numPlayers && visited_set != null; p++) {
                    if(p == id) continue;

                    Set<Integer> playerVisitedTargets = visited_set.get(p);

                    if(playerVisitedTargets.contains(targetIndex))   {
                        targetRealValue--;
                        targerProjectedValue--;
                    }
                    else {
                        if (!playerWillReachTargetFirst(p, target, groupLocations, currentLoc)) {
                            targerProjectedValue--;
                        }
                    }
                }

                double timeToTarget = Point.getDistance(currentLoc, target)/Simulator.getSpeed(Point.getDirection(currentLoc, target), wind_direction);
                double distToTarget = relativeDistance(groupLocations,target);
                double heuristic = targetRealValue*targerProjectedValue/timeToTarget;

                maxTargetValue = Math.max(maxTargetValue, heuristic);
                if(maxTargetValue == heuristic){
                    maxiIndex = targetIndex;
                    targetStats.targetIndex = targetIndex;
                    targetStats.timeToTarget = timeToTarget;
                    targetStats.distToTarget = distToTarget;
                    targetStats.heuristic = heuristic;
                }
            }
        return targetStats;
    }

    public double relativeDistance(List<Point> groupLocations, Point target){
        double dist = 0;
        for(int p=0;p<groupLocations.size();p++){

            if(p==id){
                continue;
            }
            dist+=Point.getDistance(groupLocations.get(p), target)/Simulator.getSpeed(Point.getDirection(groupLocations.get(p), target), wind_direction);
        }
        return  dist;
    }

    public boolean playerWillReachTargetFirst(int p, Point target,List<Point> groupLocations, Point currentLoc){
        double t1  = Point.getDistance(currentLoc, target)/Simulator.getSpeed(Point.getDirection(currentLoc, target), wind_direction);
        double t2 = Point.getDistance(groupLocations.get(p), target)/Simulator.getSpeed(Point.getDirection(groupLocations.get(p), target), wind_direction);
        return t1<=t2;
    }

    /*
     * Functions not currently used
     */

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

    public ArrayList<Point> findBestPath(Point currentLoc, ArrayList<Point> points) {
        
        int ps = points.size();
        double lowestDist = Double.MAX_VALUE;
        double d;
        ArrayList<Point> bestPathSoFar = null;
        for (ArrayList<Point> path : listPermutations(points)) {
            if (path.size() == ps) {
                d = getPathDistance(currentLoc, path);
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
        return findBestPathByTime(currentLoc, kClosest);
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

    public ArrayList<Point> getkOptimalTargets(ArrayList<Integer> availableTargetIndices, Point currentLoc, 
        List<Point> groupLocations, Map<Integer, Set<Integer>> visited_set, int id, int k) {

        double maxTargetValue = 0.0;
        int numPlayers = groupLocations.size();
        int maxiIndex = availableTargetIndices.get(0);
        HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
        ArrayList<Integer> bestTargets = new ArrayList<Integer>();

        for (int targetIndex : availableTargetIndices) {
            Point target = targets.get(targetIndex);

            double targetRealValue = numPlayers;
            double targerProjectedValue = numPlayers;

            for (int p = 0; p < numPlayers && visited_set != null; p++) {
                if(p == id) continue;

                Set<Integer> playerVisitedTargets = visited_set.get(p);

                if(playerVisitedTargets.contains(targetIndex))   {
                    targetRealValue--;
                    targerProjectedValue--;
                }
                else {
                    if (!playerWillReachTargetFirst(p, target, groupLocations, currentLoc)) {
                        targerProjectedValue--;
                    }
                }
            }

            double timeToTarget = Point.getDistance(currentLoc, target)/Simulator.getSpeed(Point.getDirection(currentLoc, target), wind_direction);
            double heuristic = targetRealValue*targerProjectedValue/timeToTarget;
            scores.put(targetIndex, heuristic);
        }

        double tmpMax, globalMax;
        if(scores.size() > k)  {

            for(int i=0; i<k; i++)  {                
                globalMax = -1;
                for(Integer key: scores.keySet()) {
                    tmpMax = scores.get(key);
                    
                    if(tmpMax > globalMax)  {
                        globalMax = tmpMax;
                        maxiIndex = key;
                    }
                }    
                bestTargets.add(maxiIndex);
                scores.put(maxiIndex, -1.0);            
            }
        }   
        else    {
            for(Integer key: scores.keySet()) {
                bestTargets.add(key);
            }   
        }

        int idx = 0;
        int counter = 0;
        int nextTargetIndex = 0;
        double accum = 0;
        Point target = null;
        Point nextTarget = null;
        Point nextCurrentLoc = null;
        ArrayList<Integer> tmpAvailableTargetIndices = null;
        ArrayList<Double> accumulated = new ArrayList<Double>();
        ArrayList<Point> targetSequence = new ArrayList<Point>();
        HashMap<Integer, ArrayList<Point>> kTargetSequences = new HashMap<Integer, ArrayList<Point>>();

        for(int targetIndex: bestTargets) {
            
            accum = 0;
            counter = 0;
            target = targets.get(targetIndex);
            nextTargetIndex = targetIndex;

            accumulated = new ArrayList<Double>();
            targetSequence = new ArrayList<Point>();
            tmpAvailableTargetIndices = new ArrayList<Integer>(availableTargetIndices);            
            tmpAvailableTargetIndices.remove(new Integer(targetIndex));
            accum += Point.getDistance(currentLoc, target)/Simulator.getSpeed(Point.getDirection(currentLoc, target), wind_direction);

            while(counter < k-1)  {

                targetSequence.add(target);            
                nextCurrentLoc = target;
                TargetStats targetStats = getHeuristicDistance(tmpAvailableTargetIndices, nextCurrentLoc, groupLocations, visited_set, id);
                nextTargetIndex = targetStats.targetIndex;

                target = targets.get(nextTargetIndex);
                tmpAvailableTargetIndices.remove(new Integer(nextTargetIndex));
                accum += targetStats.heuristic;
                counter++;
            }
            targetSequence.add(target);
            kTargetSequences.put(idx++, targetSequence);
            accumulated.add(accum);
        }


        double maxAccum = Collections.max(accumulated);
        ArrayList<Point> nextSequence = kTargetSequences.get(accumulated.indexOf(maxAccum));
        
        return nextSequence;
    }

}