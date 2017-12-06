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
    int numPlayers = 0;
    Point wind_direction;
    double wind_angle;
    boolean inStraightMode = false;
    SailingHelper sHelper;
    List<Point> nextKpoints;
    int k;
    int pointsVisited = 0;

    public enum InitMode { 
        CENTER, WIND 
    }
    InitMode initMode = InitMode.WIND;

    public enum ModeMode { 
        ONE_STEP, K_STEPS 
    }
    ModeMode modeMode = ModeMode.ONE_STEP;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {

        // seed to be deterministic (wrt input randomness)
        this.gen = new Random(seed);
        this.wind_direction = wind_direction;
        this.wind_angle = Math.atan2(wind_direction.y, wind_direction.x); 
        this.k = Math.max(3, t);
        this.nextKpoints = new ArrayList<Point>();

        switch(initMode)   {
            case CENTER:
                this.initial = new Point(5.0, 5.0);
                break;

            case WIND:

                if(t > 20)  {

                    // The middle point wrt the wind lies in the line that follows the wind vector, upwind direction,
                    // at 1/3 of the distance from the center (5,5) to the edges of the board.
                    // To compute it, first we need the unit vector that represents the wind direction.
                    Point unitWind = Point.getUnitVector(wind_direction);

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
                    this.initial = new Point(
                            5 + (1./3 * distanceToEdge) * unitWind.x,
                            5 + (1./3 * distanceToEdge) * unitWind.y
                    );
                }
                else    {
                    this.initial = new Point(5.0+2*Math.cos(wind_angle), 5.0+2*Math.sin(wind_angle));
                }
                
                break;
        }
        
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        
        this.id = id;
        this.targets = targets;
        this.numPlayers = group_locations.size();
        this.sHelper = new SailingHelper(targets, id, initial, wind_direction, wind_angle);        
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

        Point goalLoc = null;
        Point prevTarget = null;
        Point currentLoc = group_locations.get(id);
        ArrayList<Integer> availableTargetIndices = new ArrayList<Integer>();
        

        if(visited_set != null && visited_set.get(id).size() == targets.size()) {
            goalLoc = initial;
        } else {
            
            for(int i = 0; i < targets.size(); i++) {
                if (visited_set == null || !visited_set.get(id).contains(i)) {
                    availableTargetIndices.add(i);
                }
            }

            switch(modeMode)   {
                case K_STEPS:

                    if(nextKpoints.size() == 0) {
                        nextKpoints = sHelper.getkOptimalTargets(availableTargetIndices, currentLoc, group_locations, visited_set, this.id, k);
                    }
                    goalLoc = nextKpoints.get(0);
                    break;

                case ONE_STEP:

                    int indexNextTarget = sHelper.getHeuristicDistance(availableTargetIndices, currentLoc, group_locations, visited_set, this.id).targetIndex;
                    goalLoc = targets.get(indexNextTarget);
                    break;
            }

        }
        double relAngleWouldNeedToGoIn = sHelper.getRelAngle(currentLoc, goalLoc);        
        if (sHelper.shouldCurve(relAngleWouldNeedToGoIn) && !sHelper.closeToBoundary(currentLoc, dt)/*&& isInsideBox(newPointCurvingWouldGive)*/) {
            return sHelper.getBestAbsoluteDirection(currentLoc, goalLoc);
        } else {
            return Point.getDirection(currentLoc, goalLoc);
        }
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {

        switch(modeMode)   {
            case K_STEPS:
                if(this.visited_set != null && this.visited_set.get(id).size() > pointsVisited) {
                    pointsVisited++;
                    nextKpoints.remove(0);
                }
                break;
        }

        this.visited_set = visited_set;
    }
}
