package sail.g6;

import sail.sim.Point;
import sail.sim.Simulator;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class Player extends sail.sim.Player {
    List<Point> targets;
    List<Point> path;
    List<Point> prevLoc;
    Map<Integer, Set<Integer>> visited_set;
    Random gen;
    int id;
    Point initial;
    Point wind;

    int nextTarget = -1;

    @Override
    public Point chooseStartingLocation(Point wind_direction, Long seed, int t) {
        // you don't have to use seed unless you want it to 
        // be deterministic (wrt input randomness)
        wind = wind_direction;
        prevLoc = new ArrayList<>();
        path = new ArrayList<>();
        Point bias = Point.rotateCounterClockwise(wind_direction, Math.PI / 2);
        double bias_x = bias.x * 4;
        double bias_y = bias.y * 4;
        initial = new Point(5 + bias_x, 5 + bias_y);
        double speed = Simulator.getSpeed(initial, wind_direction);
        prevLoc.add(0, initial);
        return initial;
    }

    @Override
    public void init(List<Point> group_locations, List<Point> targets, int id) {
        this.targets = targets;
        this.id = id;
    }

    @Override
    public Point move(List<Point> group_locations, int id, double dt, long time_remaining_ms) {

        if (visited_set != null && visited_set.get(id).size() == targets.size()) {
            // This is if we have finished visiting all targets.
            nextTarget = targets.size();
            return findAngle(group_locations.get(id), initial, dt);
        }
        else {
            // System.out.println("Selecting next target.");
            // Here is the logic to decide which target to head to next.
            // Now we just consider the nearest target.
            double min = 1e9;
            int mark = 0;
            for (int i = 0; i < targets.size(); i++) {
                if (visited_set != null && visited_set.get(id).contains(i)) continue;
                int count = group_locations.size();
                for (int j = 0; j < group_locations.size(); j++) {
                    if (j == id) continue;
                    if (visited_set != null && visited_set.get(j).contains(i)) {
                        count--;
                    }
                }
                double dist = getTrueWeight(group_locations.get(id), targets.get(i)) / count;
                if (dist < min) {
                    min = dist;
                    mark = i;
                }
            }
            nextTarget = mark;
            return findAngle(group_locations.get(id), targets.get(mark), dt);
        }
    }
    
    public Point findAngle(Point currentLoc, Point nextLoc, double dt) {
        double min = 1e9;
        Point result = nextLoc;
        for (int i = 20; i < 160; i++) {
            double angle = Math.PI * (i - 90) / 180;
            Point direction = Point.getDirection(currentLoc, nextLoc);
            Point rotation = Point.rotateCounterClockwise(direction, angle);
            Point step = Point.multiply(rotation, dt);
            Point median = Point.sum(currentLoc, step);
            if (median.x < 0.1 || median.x > 9.9 || median.y < 0.1 || median.y > 9.9) continue;
            double temp = getTrueWeight(currentLoc, median) + getTrueWeight(median, nextLoc);
            if (temp < min) {
                min = temp;
                result = median;
            }
        }
        return moveInPath(currentLoc, result);
    }

    public Point findPathAndMove(Point currentLoc, Point nextLoc) {
        ArrayList<Point> grid = getGrid(currentLoc, nextLoc);

        double minDist = getTrueWeight(currentLoc, nextLoc);
        int minIndex = -1;

        for (int i=0; i < grid.size(); ++i) {
            if (getTrueWeight(currentLoc, grid.get(i))
                    + getTrueWeight(grid.get(i), nextLoc) < minDist) {
                minIndex = i;
                minDist = getTrueWeight(currentLoc, grid.get(i))
                        + getTrueWeight(grid.get(i), nextLoc);
            }
        }

        // Direct path is the best path.
        if (minIndex == -1) {
            //System.out.println("Direct path is the best.");
            path.add(nextLoc);
            return moveInPath(currentLoc, nextLoc);
        }

        //System.out.println("Current point: " + currentLoc.x + " " + currentLoc.y);
        //System.out.println("Destination point: " + nextLoc.x + " " + nextLoc.y);
        //System.out.println("Taking a deviation through: " + grid.get(minIndex).x + " " + grid.get(minIndex).y);
        path.add(grid.get(minIndex));
        path.add(nextLoc);

        return moveInPath(currentLoc, grid.get(minIndex));
    }

    // Get the grid of intermediate points.
    private ArrayList<Point> getGrid(Point currentLoc, Point nextLoc) {
        double maxLength = Point.getDistance(currentLoc, nextLoc);
        double step = 0.04;    // TODO: Determine the best step size.
        int gridLength = 40;    // TODO: Determine the optimum number of points.

        ArrayList<Point> grid = new ArrayList<Point>();

        Point p = new Point(nextLoc.x - (gridLength/2) * step, nextLoc.y - (gridLength/2) * step);

        for (int i = 0; i < gridLength; ++i) {
            for (int j = 0; j < gridLength; ++j) {
                double x = p.x + i*step;
                double y = p.y + j*step;

                // Check if it's an invalid point.
                if (x < 0.1 || y < 0.1 || x > 9.9 || y > 9.9) {
                    continue;
                }

                Point p1 = new Point(x, y);
                if (Point.getDistance(currentLoc, p1) >= maxLength) {
                    continue;
                }

                grid.add(p1);
            }
        }

        // System.out.println("Grid size: " + grid.size());

        return grid;
    }

    // Below 2 methods are borrowed from the simulator.
    // checks if (a --- > b) line segment intersects circle radius 0.01 around t
    private static boolean checkCrossedInterpolation(Point t, Point a, Point b) {
        Point p = getInterpolatedIntersectionPoint(t, a, b);
        if(p.x == -1 && p.y == -1) return false;
        else return true;
    }

    private static Point getInterpolatedIntersectionPoint(Point centre, Point e, Point l) {
        double r = 0.01;
        Point d = Point.getDirection(e, l);
        Point f = Point.getDirection(centre, e);
        double a = Point.dot(d,d);
        double b = 2 * Point.dot(f,d);
        double c = Point.dot(f,f) - r*r;
        double discr = b*b - 4*a*c;
        if(discr < 0) {
            return new Point(-1, -1);
        }
        discr = Math.sqrt(discr);
        double t1 = (-b - discr)/(2*a);
        double t2 = (-b + discr)/(2*a);


        if(t1 >=0 && t1 <= 1) {
            return Point.sum(e, Point.multiply(d,t1));
        } else if(t2 >= 0 && t2 <= 1) {
            return new Point(-1, -1);
        } else {
            return new Point(-1, -1);
        }
    }

    // Move from currentLoc to nextLoc.
    private Point moveInPath(Point currentLoc, Point nextLoc) {
        return Point.getDirection(currentLoc, nextLoc);
    }

    // Gets the distance between 2 points scaled by the wind direction.
    private double getTrueWeight(Point currentLoc, Point nextLoc) {
        double dist = Point.getNorm(Point.getDirection(currentLoc, nextLoc));
        double angle = Point.angleBetweenVectors(Point.getDirection(currentLoc, nextLoc), wind);
        double speed = calcSpeed(angle);

        return dist/speed;
    }

    // Gets the speed factor in the direction of the angle.
    private double calcSpeed(double angle) {
        Point p = new Point(2.5*Math.cos(angle) - 0.5, 5 * Math.sin(angle));
        return Point.getDistance(p, new Point(0, 0));
    }

    /**
    * visited_set.get(i) is a set of targets that the ith player has visited.
    */
    @Override
    public void onMoveFinished(List<Point> group_locations, Map<Integer, Set<Integer>> visited_set) {
        this.visited_set = visited_set;
    }
}
